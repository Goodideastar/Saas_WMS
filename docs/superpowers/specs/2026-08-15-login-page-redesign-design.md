# WMS Login Page - Tech Warehouse Style

## Overview

Redesign the WMS login page with subtle high-tech warehouse aesthetic while keeping the existing left-right split layout and all functional behavior unchanged.

## Design Decisions

- **Animation intensity**: Subtle — light particle drift + scan line, no heavy WebGL
- **Color scheme**: Blue-cyan tech (#0a0f1e deep midnight blue base, #06b6d4 cyan accent, #3b82f6 blue)
- **Layout**: Keep current left (brand) + right (form) split
- **No new dependencies**: All effects via inline Canvas + CSS animations

## Specifications

### Left Panel (Brand Area, 260px)

1. **Particle Canvas**
   - ~30 particles, 1-3px radius, cyan/blue semi-transparent
   - Drift slowly with random velocity, bounce off edges
   - requestAnimationFrame driven
   - Color: rgba(6, 182, 212, 0.3~0.6)

2. **Scan Line**
   - Single horizontal cyan line, opacity ~8%
   - CSS animation: top → bottom, 4s linear infinite
   - 2px height, full width of panel

3. **Warehouse Wireframe Icon**
   - Subtle SVG rack/shelf outline in background (10% opacity)
   - Bottom-right corner of left panel

4. **Mini Dashboard Decoration**
   - Small numeric readout in bottom-left: e.g. "SYS ONLINE · 2026"
   - Cyan monospace text, 60% opacity

### Right Panel (Login Form, 400px)

1. **Top Accent Bar**
   - 2px gradient line at top: cyan → blue

2. **Logo Enhancement**
   - Keep existing SVG warehouse icon
   - Add CSS breathing glow: box-shadow pulse, 2s ease-in-out infinite

3. **Input Fields**
   - Focus state: cyan border glow + bottom progress bar fill animation

4. **Login Button**
   - Background: linear-gradient(135deg, #06b6d4, #3b82f6)
   - Hover: brightness 1.1 + subtle shine sweep (pseudo-element)
   - Active: scale 0.98

### Color Palette

| Token | Value |
|-------|-------|
| bg-deep | #0a0f1e |
| bg-card | #111827 |
| accent-cyan | #06b6d4 |
| accent-blue | #3b82f6 |
| text-primary | #e2e8f0 |
| text-muted | rgba(148, 163, 184, 0.7) |
| border-subtle | rgba(6, 182, 212, 0.15) |

### File

- Single file change: `frontend/src/views/Login.vue`
- No new files, no new dependencies
- Existing JS logic (login flow, form validation, route redirect) unchanged
