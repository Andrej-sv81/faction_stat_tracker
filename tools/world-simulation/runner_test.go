package main

import (
	"bytes"
	"context"
	"encoding/json"
	"math/rand"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestGenerateRandomEventUsesEventSpecificEffectsAndSeverity(t *testing.T) {
	random := rand.New(rand.NewSource(17))
	factions := []Faction{{ID: "faction-1", Name: "Ember Hold"}}
	eventTypes := []EventType{{
		Code:        "DISCOVERY",
		Name:        "Discovery",
		Description: "A hidden artifact is uncovered.",
	}}

	event := generateRandomEvent(random, 4, factions, eventTypes)

	if event.Type != "DISCOVERY" {
		t.Fatalf("event type = %q, want DISCOVERY", event.Type)
	}
	if event.TargetFactionID != "faction-1" || event.SecondTargetFaction != nil {
		t.Fatalf("unexpected targets: %+v", event)
	}
	if event.Severity != 4 {
		t.Fatalf("severity = %d, want 4", event.Severity)
	}
	if !strings.Contains(event.Details, "Ember Hold") &&
		!strings.Contains(event.Title, "Ember Hold") {
		t.Fatalf("event should name its faction: %+v", event)
	}

	if event.PowerDelta != 0 || event.WealthDelta != 0 || event.ManpowerDelta != 0 {
		t.Fatalf("discovery should not damage unrelated stats: %+v", event)
	}
	if event.InfluenceDelta <= 0 || event.TerritoryDelta <= 0 {
		t.Fatalf("discovery should improve influence and territory: %+v", event)
	}
	for _, delta := range []int{event.InfluenceDelta, event.TerritoryDelta} {
		magnitude := delta
		if magnitude < 0 {
			magnitude = -magnitude
		}
		if magnitude < 8 || magnitude > 16 {
			t.Fatalf("delta out of severity 4 range: %d", delta)
		}
	}
	if event.TerritoryDelta > 20 {
		t.Fatalf("territory delta exceeds API bound: %d", event.TerritoryDelta)
	}
}

func TestGenerateRandomEventUsesLogicalEffectsForCatalogTypes(t *testing.T) {
	tests := []struct {
		name        string
		code        string
		domain      string
		checkDeltas func(*testing.T, EventRequest)
	}{
		{
			name:   "alliance strengthens manpower and influence",
			code:   "ALLIANCE",
			domain: "Politics, Diplomacy & Leadership",
			checkDeltas: func(t *testing.T, event EventRequest) {
				if event.ManpowerDelta <= 0 || event.InfluenceDelta <= 0 || event.WealthDelta != 0 {
					t.Fatalf("unexpected alliance deltas: %+v", event)
				}
			},
		},
		{
			name:   "monetary gain increases wealth",
			code:   "MONETARY_GAIN",
			domain: "Economy & Resources",
			checkDeltas: func(t *testing.T, event EventRequest) {
				if event.WealthDelta <= 0 || event.ManpowerDelta != 0 {
					t.Fatalf("unexpected monetary-gain deltas: %+v", event)
				}
			},
		},
		{
			name:   "conflict costs manpower and wealth",
			code:   "CONFLICT",
			domain: "Warfare & Security",
			checkDeltas: func(t *testing.T, event EventRequest) {
				if event.ManpowerDelta >= 0 || event.WealthDelta >= 0 {
					t.Fatalf("unexpected conflict deltas: %+v", event)
				}
			},
		},
		{
			name:   "unknown types use their domain profile",
			code:   "NEW_WAR_EVENT",
			domain: "Warfare & Security",
			checkDeltas: func(t *testing.T, event EventRequest) {
				if event.ManpowerDelta >= 0 || event.WealthDelta >= 0 {
					t.Fatalf("unexpected fallback deltas: %+v", event)
				}
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			event := generateRandomEvent(
				rand.New(rand.NewSource(21)),
				3,
				[]Faction{{ID: "faction-1", Name: "Ember Hold"}},
				[]EventType{{Code: test.code, Domain: test.domain}},
			)
			test.checkDeltas(t, event)
		})
	}
}

func TestAPIClientCreatesAuthenticatedSeverityEventWithCSRF(t *testing.T) {
	var posted EventRequest
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		switch request.URL.Path {
		case "/api/auth/csrf":
			http.SetCookie(writer, &http.Cookie{Name: "XSRF-TOKEN", Value: "csrf-123", Path: "/"})
			_, _ = writer.Write([]byte(`{"headerName":"X-XSRF-TOKEN","token":"csrf-123"}`))
		case "/api/worlds/world-1":
			if request.Header.Get("Authorization") != "Bearer access-token" {
				t.Errorf("missing bearer token: %q", request.Header.Get("Authorization"))
			}
			_, _ = writer.Write([]byte(`{"factions":[{"id":"faction-1","name":"Ember Hold"}]}`))
		case "/api/event-types":
			_, _ = writer.Write([]byte(`[{"code":"DISCOVERY","name":"Discovery","domain":"Knowledge","description":"An artifact is found."}]`))
		case "/api/worlds/world-1/events":
			cookie, err := request.Cookie("XSRF-TOKEN")
			if err != nil || cookie.Value != "csrf-123" {
				t.Errorf("missing CSRF cookie: %v", err)
			}
			if request.Header.Get("X-XSRF-TOKEN") != "csrf-123" {
				t.Errorf("missing CSRF header: %q", request.Header.Get("X-XSRF-TOKEN"))
			}
			if err := json.NewDecoder(request.Body).Decode(&posted); err != nil {
				t.Errorf("decode event request: %v", err)
			}
			writer.WriteHeader(http.StatusCreated)
			_, _ = writer.Write([]byte(`{"title":"Discovery — Ember Hold"}`))
		default:
			http.NotFound(writer, request)
		}
	}))
	defer server.Close()

	client, err := NewAPIClient(server.URL, "world-1", "access-token")
	if err != nil {
		t.Fatal(err)
	}
	title, err := client.CreateRandomEvent(
		context.Background(),
		4,
		rand.New(rand.NewSource(17)),
	)
	if err != nil {
		t.Fatalf("CreateRandomEvent returned error: %v", err)
	}
	if title != "Discovery — Ember Hold" {
		t.Fatalf("title = %q", title)
	}
	if posted.Type != "DISCOVERY" || posted.TargetFactionID != "faction-1" {
		t.Fatalf("unexpected generated event: %+v", posted)
	}
	if posted.Severity != 4 {
		t.Fatalf("severity = %d, want 4", posted.Severity)
	}
	if posted.InfluenceDelta <= 0 || posted.TerritoryDelta <= 0 {
		t.Fatalf("discovery should improve influence and territory: %+v", posted)
	}
}

func TestRunSupportsPausePlayAndConfigurationCommands(t *testing.T) {
	var output bytes.Buffer
	client, err := NewAPIClient("http://localhost:8080", "world-1", "test-token")
	if err != nil {
		t.Fatal(err)
	}

	err = Run(
		context.Background(),
		client,
		time.Minute,
		3,
		strings.NewReader("interval 2\nseverity 5\nstatus\nplay\npause\nquit\n"),
		&output,
	)
	if err != nil {
		t.Fatalf("Run returned error: %v", err)
	}

	for _, expected := range []string{
		"Interval set to 2 minutes.",
		"Severity set to 5.",
		"Status: paused; interval: 2m0s; severity: 5.",
		"Playing; first event in 2m0s.",
		"Paused.",
	} {
		if !strings.Contains(output.String(), expected) {
			t.Errorf("output missing %q:\n%s", expected, output.String())
		}
	}
}
