package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	apiURL := flag.String("api", "http://localhost:8080", "Backend API base URL")
	worldID := flag.String("world", "", "World ID to simulate")
	token := flag.String("token", os.Getenv("FACTION_STAT_TRACKER_TOKEN"), "Bearer access token")
	intervalMinutes := flag.Int("interval-minutes", 5, "Minutes between events (1-1440)")
	severity := flag.Int("severity", 3, "Event severity (1-5)")
	flag.Parse()

	if *worldID == "" {
		fmt.Fprintln(os.Stderr, "world ID is required; use -world")
		os.Exit(2)
	}
	if *token == "" {
		fmt.Fprintln(os.Stderr, "access token is required; set FACTION_STAT_TRACKER_TOKEN or use -token")
		os.Exit(2)
	}
	interval := time.Duration(*intervalMinutes) * time.Minute
	if err := validateSettings(interval, *severity); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	client, err := NewAPIClient(*apiURL, *worldID, *token)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	fmt.Fprintf(os.Stdout, "World simulation ready for %s. Starts paused.\n", *worldID)
	if err := Run(ctx, client, interval, *severity, os.Stdin, os.Stdout); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
