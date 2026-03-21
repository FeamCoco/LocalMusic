# 2026-03-21 Material 3 Redesign Decision

## Team Roles

- Product lead: Codex
- UI designer: sub-agent `Hume`
- UX designer: sub-agent `Fermat`
- Android engineer: sub-agent `Beauvoir`
- Android architect reviewer: sub-agent `Parfit` (completed)

## Final Product Decision

### App Name

- Chinese: `连音`
- English reference: `LinkTone`

Decision rationale:

- It directly communicates the product's two strongest jobs: local music playback and device connection.
- It is clearer than the current name for first-time users.
- It supports a calmer, more device-oriented Material 3 visual direction.

### Product Positioning

`连音` is a calm offline music player focused on local playback, queue control, Bluetooth device handoff, and sleep timer utility.

## Design Direction

### Visual Tone

- Quiet
- Reliable
- Light technology feel
- Reduced visual noise

### Material 3 Principles To Enforce

- Use Material 3 surface hierarchy instead of large custom full-screen gradients
- Keep primary color for emphasis, not for painting the whole screen
- Use clear component hierarchy: primary action, secondary action, destructive action
- Prefer large touch-friendly controls in the thumb zone
- Use cards, list rows, chips, and bottom sheets with consistent corner radii
- Support dynamic color on supported Android versions, but keep a clear fallback brand palette

### Brand Palette

- Brand family: deep teal / sea-glass / cool slate
- Mood: stable, low-noise, modern
- Accent from artwork remains local to the player surface, not the entire app shell

## UX Priorities

### Priority Tasks

1. Continue playback and understand current playback state
2. Switch tracks and manage the queue
3. Connect or disconnect Bluetooth audio devices
4. Set or cancel sleep timer
5. Use low-frequency settings such as notification style

### UX Rules

- P0 and P1 actions must be visible without entering overflow menus
- Empty states must explain the next step, not only the problem
- Destructive actions must default to the safer option
- Connection and scan states must be explicit and visible
- Hidden gestures should not be required for important actions

## Screen Decisions

### Main Player

- Keep a lightweight top app bar
- Make album art and track metadata the hero region
- Promote previous, play/pause, and next to the primary control row
- Move queue, repeat mode, Bluetooth, and sleep timer into clearly visible secondary controls
- Add a visible utility action row for scan library, Bluetooth devices, sleep timer, and notification style
- Rework empty queue into a full empty state with clear CTA
- Keep queue preview, but reduce its visual weight relative to the now-playing area

### Bluetooth Manager

- Remove the `Switch` from the top app bar
- Add a status card at the top for Bluetooth state, scanning state, and the main toggle
- Keep paired and discovered devices as separate sections
- Give device rows clearer state labels and clearer main actions
- Use page-level snackbar feedback for transient connection messages
- Show distinct empty states for Bluetooth off, scanning, and no results

### Error Screen

- Replace the white crash page with a Material 3 recovery screen
- Use professional copy instead of casual emotive wording
- Keep the current exit-app action
- Present the error page as a recovery boundary, not a raw failure surface

## Implementation Scope For This Iteration

- Update shared Compose theme toward Material 3 recommendations
- Redesign main player page
- Redesign Bluetooth page
- Redesign error page
- Rename app display name and refresh key user-facing copy
- Improve delete confirmation messaging on queue actions

## Explicit Non-Goals

- No business-layer rewrite
- No navigation architecture rewrite
- No service/media-session redesign
- No broad XML/AppCompat cleanup outside files directly needed for this redesign

## Review Gate

The Android architect review should verify:

- No business regression in playback, queue, Bluetooth, or timer flows
- Visual hierarchy now reflects Material 3 intent
- Shared theme is more complete and reusable than before
- New UI avoids hard-coded one-off colors where reasonable
- Changes stay compatible with the ongoing Compose migration
