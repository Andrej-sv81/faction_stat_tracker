package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"strconv"
	"strings"
	"time"
)

const (
	minimumSeverity = 1
	maximumSeverity = 5
	minimumInterval = time.Minute
	maximumInterval = 24 * time.Hour
)

type APIClient struct {
	baseURL    string
	worldID    string
	token      string
	httpClient *http.Client
}

type APIError struct {
	StatusCode int
	Message    string
}

func (e *APIError) Error() string {
	if e.Message == "" {
		return fmt.Sprintf("backend returned HTTP %d", e.StatusCode)
	}
	return fmt.Sprintf("backend returned HTTP %d: %s", e.StatusCode, e.Message)
}

type Faction struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type EventType struct {
	Code        string `json:"code"`
	Name        string `json:"name"`
	Domain      string `json:"domain"`
	Description string `json:"description"`
}

type WorldData struct {
	Factions []Faction `json:"factions"`
}

type csrfResponse struct {
	HeaderName string `json:"headerName"`
	Token      string `json:"token"`
}

type EventRequest struct {
	Title               string  `json:"title"`
	Type                string  `json:"type"`
	TargetFactionID     string  `json:"targetFactionId"`
	SecondTargetFaction *string `json:"secondTargetFactionId,omitempty"`
	PowerDelta          int     `json:"powerDelta"`
	WealthDelta         int     `json:"wealthDelta"`
	ManpowerDelta       int     `json:"manpowerDelta"`
	InfluenceDelta      int     `json:"influenceDelta"`
	TerritoryDelta      int     `json:"territoryDelta"`
	Details             string  `json:"details"`
	Severity            int     `json:"severity"`
}

type statDeltas struct {
	power     int
	wealth    int
	manpower  int
	influence int
	territory int
}

type impactProfile struct {
	power     int
	wealth    int
	manpower  int
	influence int
	territory int
}

var eventImpactProfiles = map[string]impactProfile{
	"CHANGE_OF_LEADERSHIP":   {power: -1, influence: -1},
	"ALLIANCE":               {manpower: 1, influence: 1},
	"BETRAYAL":               {power: -1, influence: -1},
	"DIPLOMATIC_INCIDENT":    {wealth: -1, influence: -1},
	"ANNEXATION":             {wealth: -1, manpower: 1, territory: 1},
	"SCHISM":                 {manpower: -1, influence: -1},
	"REBELLION":              {power: -1, wealth: -1, manpower: -1},
	"CONFLICT":               {power: -1, wealth: -1, manpower: -1},
	"ESPIONAGE":              {power: 1, wealth: -1, influence: 1},
	"FORTIFICATION":          {power: 1, wealth: -1},
	"PIRACY_OR_BANDITRY":     {wealth: -1, manpower: -1},
	"MERCENARY_ACTION":       {power: 1, wealth: -1, manpower: 1},
	"MONETARY_GAIN":          {wealth: 1},
	"FINANCIAL_CRISIS":       {wealth: -1, influence: -1},
	"CONTRACT":               {wealth: 1, influence: 1},
	"RESOURCE_BOOM":          {wealth: 1, manpower: 1},
	"SCARCITY":               {wealth: -1, manpower: -1},
	"INFRASTRUCTURE_PROJECT": {power: 1, wealth: -1, territory: 1},
	"DISCOVERY":              {influence: 1, territory: 1},
	"ARCANE_ANOMALY":         {power: -1, territory: -1},
	"INNOVATION":             {power: 1, wealth: 1},
	"HERESY_OR_PROPHECY":     {power: -1, influence: 1},
	"NATURAL_DISASTER":       {wealth: -1, manpower: -1, territory: -1},
	"PLAGUE":                 {wealth: -1, manpower: -1},
	"MIGRATION":              {wealth: 1, manpower: 1},
	"CULTURAL_RENAISSANCE":   {wealth: 1, influence: 1},
	"CRIME_WAVE":             {wealth: -1, influence: -1},
	"MONSTER_INCURSION":      {power: -1, manpower: -1, territory: -1},
	"DIVINE_INTERVENTION":    {power: 1, influence: 1},
	"RELIC_AWAKENING":        {power: 1, wealth: -1, influence: 1},
}

type inputLine struct {
	value string
	err   error
}

func NewAPIClient(baseURL, worldID, token string) (*APIClient, error) {
	jar, err := cookiejar.New(nil)
	if err != nil {
		return nil, fmt.Errorf("create cookie jar: %w", err)
	}
	return &APIClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		worldID: worldID,
		token:   token,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
			Jar:     jar,
		},
	}, nil
}

func (c *APIClient) SetToken(token string) {
	c.token = token
}

func (c *APIClient) CreateRandomEvent(
	ctx context.Context,
	severity int,
	random *rand.Rand,
) (string, error) {
	if severity < minimumSeverity || severity > maximumSeverity {
		return "", fmt.Errorf("severity must be between %d and %d", minimumSeverity, maximumSeverity)
	}

	worldPath := "/api/worlds/" + url.PathEscape(c.worldID)
	var world WorldData
	if err := c.request(ctx, http.MethodGet, worldPath, nil, &world); err != nil {
		return "", err
	}
	if len(world.Factions) == 0 {
		return "", errors.New("world has no factions to target")
	}

	var eventTypes []EventType
	if err := c.request(ctx, http.MethodGet, "/api/event-types", nil, &eventTypes); err != nil {
		return "", err
	}
	if len(eventTypes) == 0 {
		return "", errors.New("no enabled event types are available")
	}

	event := generateRandomEvent(random, severity, world.Factions, eventTypes)
	var result struct {
		Title string `json:"title"`
	}
	if err := c.request(
		ctx,
		http.MethodPost,
		worldPath+"/events",
		event,
		&result,
	); err != nil {
		return "", err
	}
	if result.Title != "" {
		return result.Title, nil
	}
	return event.Title, nil
}

func (c *APIClient) request(
	ctx context.Context,
	method string,
	path string,
	body any,
	result any,
) error {
	var requestBody io.Reader
	if body != nil {
		encoded, err := json.Marshal(body)
		if err != nil {
			return fmt.Errorf("encode API request: %w", err)
		}
		requestBody = bytes.NewReader(encoded)
	}
	var csrf csrfResponse
	if method != http.MethodGet && method != http.MethodHead {
		if err := c.request(ctx, http.MethodGet, "/api/auth/csrf", nil, &csrf); err != nil {
			return fmt.Errorf("get CSRF token: %w", err)
		}
		if csrf.HeaderName == "" || csrf.Token == "" {
			return errors.New("backend returned an empty CSRF token")
		}
	}

	request, err := http.NewRequestWithContext(ctx, method, c.baseURL+path, requestBody)
	if err != nil {
		return fmt.Errorf("create API request: %w", err)
	}
	request.Header.Set("Accept", "application/json")
	request.Header.Set("Authorization", "Bearer "+c.token)
	if body != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	if csrf.HeaderName != "" {
		request.Header.Set(csrf.HeaderName, csrf.Token)
	}

	response, err := c.httpClient.Do(request)
	if err != nil {
		return fmt.Errorf("call backend API: %w", err)
	}
	defer response.Body.Close()

	responseBody, err := io.ReadAll(io.LimitReader(response.Body, 4096))
	if err != nil {
		return fmt.Errorf("read backend response: %w", err)
	}
	if response.StatusCode < http.StatusOK || response.StatusCode >= http.StatusMultipleChoices {
		return &APIError{StatusCode: response.StatusCode, Message: strings.TrimSpace(string(responseBody))}
	}
	if result != nil && len(responseBody) > 0 {
		if err := json.Unmarshal(responseBody, result); err != nil {
			return fmt.Errorf("decode backend response: %w", err)
		}
	}
	return nil
}

func generateRandomEvent(
	random *rand.Rand,
	severity int,
	factions []Faction,
	eventTypes []EventType,
) EventRequest {
	eventType := eventTypes[random.Intn(len(eventTypes))]
	first := factions[random.Intn(len(factions))]
	targets := []Faction{first}
	if len(factions) > 1 && random.Intn(4) == 0 {
		offset := 1 + random.Intn(len(factions)-1)
		targets = append(targets, factions[(indexOfFaction(factions, first)+offset)%len(factions)])
	}

	profile := impactProfileFor(eventType, random)
	maximumDelta := severity * 4
	minimumDelta := severity * 2
	magnitude := minimumDelta + random.Intn(maximumDelta-minimumDelta+1)
	deltas := profile.scale(magnitude)

	names := make([]string, len(targets))
	for index, faction := range targets {
		names[index] = faction.Name
	}
	title := truncateRunes(eventType.Name+" — "+strings.Join(names, " and "), 120)
	targetFactionID := first.ID
	request := EventRequest{
		Title:           title,
		Type:            eventType.Code,
		TargetFactionID: targetFactionID,
		Details:         narrative(eventType, targets, deltas),
		Severity:        severity,
		PowerDelta:      deltas.power,
		WealthDelta:     deltas.wealth,
		ManpowerDelta:   deltas.manpower,
		InfluenceDelta:  deltas.influence,
		TerritoryDelta:  deltas.territory,
	}
	if len(targets) == 2 {
		secondID := targets[1].ID
		request.SecondTargetFaction = &secondID
	}
	return request
}

func impactProfileFor(eventType EventType, random *rand.Rand) impactProfile {
	code := strings.ToUpper(strings.TrimSpace(eventType.Code))
	if profile, exists := eventImpactProfiles[code]; exists {
		if code == "MIGRATION" || code == "DIVINE_INTERVENTION" {
			direction := 1
			if random.Intn(2) == 0 {
				direction = -1
			}
			return profile.multiply(direction)
		}
		return profile
	}

	domain := strings.ToLower(eventType.Domain)
	switch {
	case strings.Contains(domain, "warfare"), strings.Contains(domain, "threat"):
		return impactProfile{wealth: -1, manpower: -1}
	case strings.Contains(domain, "economy"):
		return impactProfile{wealth: 1}
	case strings.Contains(domain, "environment"):
		return impactProfile{wealth: -1, manpower: -1}
	case strings.Contains(domain, "knowledge"):
		return impactProfile{power: 1, influence: 1}
	default:
		return impactProfile{influence: 1}
	}
}

func (profile impactProfile) scale(magnitude int) statDeltas {
	return statDeltas{
		power:     profile.power * magnitude,
		wealth:    profile.wealth * magnitude,
		manpower:  profile.manpower * magnitude,
		influence: profile.influence * magnitude,
		territory: profile.territory * magnitude,
	}
}

func (profile impactProfile) multiply(factor int) impactProfile {
	return impactProfile{
		power:     profile.power * factor,
		wealth:    profile.wealth * factor,
		manpower:  profile.manpower * factor,
		influence: profile.influence * factor,
		territory: profile.territory * factor,
	}
}

func indexOfFaction(factions []Faction, target Faction) int {
	for index, faction := range factions {
		if faction.ID == target.ID {
			return index
		}
	}
	return 0
}

func narrative(eventType EventType, factions []Faction, deltas statDeltas) string {
	impacts := make([]string, 0, 5)
	for _, stat := range []struct {
		name  string
		value int
	}{
		{name: "Power", value: deltas.power},
		{name: "Wealth", value: deltas.wealth},
		{name: "Manpower", value: deltas.manpower},
		{name: "Influence", value: deltas.influence},
		{name: "Territory", value: deltas.territory},
	} {
		if stat.value == 0 {
			continue
		}
		suffix := ""
		if stat.name == "Territory" {
			suffix = "%"
		}
		impacts = append(impacts, fmt.Sprintf("%s %+d%s", stat.name, stat.value, suffix))
	}
	impact := strings.Join(impacts, ", ")
	if len(factions) == 2 {
		impact = "Both factions: " + impact
	}
	description := strings.TrimSpace(strings.TrimSuffix(eventType.Description, "."))
	if eventType.Description == "" {
		description = eventType.Name
	}
	return fmt.Sprintf("%s. Effects: %s.", description, impact)
}

func truncateRunes(value string, limit int) string {
	runes := []rune(value)
	if len(runes) <= limit {
		return value
	}
	return string(runes[:limit])
}

func validateSettings(interval time.Duration, severity int) error {
	if interval < minimumInterval || interval > maximumInterval {
		return fmt.Errorf("interval must be between 1 and 1440 minutes")
	}
	if severity < minimumSeverity || severity > maximumSeverity {
		return fmt.Errorf("severity must be between %d and %d", minimumSeverity, maximumSeverity)
	}
	return nil
}

func Run(
	ctx context.Context,
	client *APIClient,
	interval time.Duration,
	severity int,
	input io.Reader,
	output io.Writer,
) error {
	if err := validateSettings(interval, severity); err != nil {
		return err
	}

	commands := make(chan inputLine)
	done := make(chan struct{})
	defer close(done)
	go scanCommands(input, commands, done)

	timer := time.NewTimer(time.Hour)
	if !timer.Stop() {
		<-timer.C
	}
	var timerChannel <-chan time.Time
	running := false
	random := rand.New(rand.NewSource(time.Now().UnixNano()))

	fmt.Fprintln(output, "Commands: play, pause, interval <minutes>, severity <1-5>, status, token <access-token>, quit")
	for {
		select {
		case <-ctx.Done():
			return nil
		case line, open := <-commands:
			if !open {
				return line.err
			}
			if line.err != nil {
				return line.err
			}
			parts := strings.Fields(line.value)
			if len(parts) == 0 {
				continue
			}
			switch strings.ToLower(parts[0]) {
			case "play":
				if !running {
					running = true
					resetTimer(timer, interval, &timerChannel)
					fmt.Fprintf(output, "Playing; first event in %s.\n", interval)
				}
			case "pause":
				running = false
				stopTimer(timer, &timerChannel)
				fmt.Fprintln(output, "Paused.")
			case "interval":
				if len(parts) != 2 {
					fmt.Fprintln(output, "Usage: interval <minutes>")
					continue
				}
				minutes, err := strconv.Atoi(parts[1])
				if err != nil {
					fmt.Fprintln(output, "Interval must be a whole number of minutes.")
					continue
				}
				updated := time.Duration(minutes) * time.Minute
				if err := validateSettings(updated, severity); err != nil {
					fmt.Fprintln(output, err)
					continue
				}
				interval = updated
				if running {
					resetTimer(timer, interval, &timerChannel)
				}
				fmt.Fprintf(output, "Interval set to %d minutes.\n", minutes)
			case "severity":
				if len(parts) != 2 {
					fmt.Fprintln(output, "Usage: severity <1-5>")
					continue
				}
				updated, err := strconv.Atoi(parts[1])
				if err != nil || updated < minimumSeverity || updated > maximumSeverity {
					fmt.Fprintln(output, "Severity must be between 1 and 5.")
					continue
				}
				severity = updated
				fmt.Fprintf(output, "Severity set to %d.\n", severity)
			case "status":
				state := "paused"
				if running {
					state = "playing"
				}
				fmt.Fprintf(output, "Status: %s; interval: %s; severity: %d.\n", state, interval, severity)
			case "token":
				if len(parts) != 2 {
					fmt.Fprintln(output, "Usage: token <access-token>")
					continue
				}
				client.SetToken(parts[1])
				fmt.Fprintln(output, "Access token updated.")
			case "quit", "exit":
				return nil
			default:
				fmt.Fprintln(output, "Unknown command.")
			}
		case <-timerChannel:
			title, err := client.CreateRandomEvent(ctx, severity, random)
			if err != nil {
				fmt.Fprintf(output, "Event creation failed: %v\n", err)
				var apiError *APIError
				if errors.As(err, &apiError) &&
					(apiError.StatusCode == http.StatusUnauthorized ||
						apiError.StatusCode == http.StatusForbidden) {
					running = false
					stopTimer(timer, &timerChannel)
					fmt.Fprintln(output, "Paused; update the access token and use play to resume.")
					continue
				}
			} else {
				fmt.Fprintf(output, "Event recorded: %s\n", title)
			}
			if running {
				resetTimer(timer, interval, &timerChannel)
			}
		}
	}
}

func resetTimer(timer *time.Timer, duration time.Duration, timerChannel *<-chan time.Time) {
	stopTimer(timer, timerChannel)
	timer.Reset(duration)
	*timerChannel = timer.C
}

func stopTimer(timer *time.Timer, timerChannel *<-chan time.Time) {
	if !timer.Stop() {
		select {
		case <-timer.C:
		default:
		}
	}
	*timerChannel = nil
}

func scanCommands(input io.Reader, commands chan<- inputLine, done <-chan struct{}) {
	defer close(commands)
	scanner := bufio.NewScanner(input)
	for scanner.Scan() {
		select {
		case commands <- inputLine{value: scanner.Text()}:
		case <-done:
			return
		}
	}
	if err := scanner.Err(); err != nil {
		select {
		case commands <- inputLine{err: fmt.Errorf("read command: %w", err)}:
		case <-done:
		}
	}
}
