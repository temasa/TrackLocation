---
name: Kinetic Precision
colors:
  surface: '#0e150e'
  surface-dim: '#0e150e'
  surface-bright: '#333b33'
  surface-container-lowest: '#091009'
  surface-container-low: '#161d16'
  surface-container: '#1a221a'
  surface-container-high: '#242c24'
  surface-container-highest: '#2f372e'
  on-surface: '#dce5d9'
  on-surface-variant: '#bccbb9'
  inverse-surface: '#dce5d9'
  inverse-on-surface: '#2a322a'
  outline: '#869585'
  outline-variant: '#3d4a3d'
  surface-tint: '#4ae176'
  primary: '#4be277'
  on-primary: '#003915'
  primary-container: '#22c55e'
  on-primary-container: '#004b1e'
  inverse-primary: '#006e2f'
  secondary: '#ddb7ff'
  on-secondary: '#490080'
  secondary-container: '#6f00be'
  on-secondary-container: '#d6a9ff'
  tertiary: '#95d4ba'
  on-tertiary: '#003829'
  tertiary-container: '#7ab8a0'
  on-tertiary-container: '#004937'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6bff8f'
  primary-fixed-dim: '#4ae176'
  on-primary-fixed: '#002109'
  on-primary-fixed-variant: '#005321'
  secondary-fixed: '#f0dbff'
  secondary-fixed-dim: '#ddb7ff'
  on-secondary-fixed: '#2c0051'
  on-secondary-fixed-variant: '#6900b3'
  tertiary-fixed: '#b0f0d6'
  tertiary-fixed-dim: '#95d3ba'
  on-tertiary-fixed: '#002117'
  on-tertiary-fixed-variant: '#0b513d'
  background: '#0e150e'
  on-background: '#dce5d9'
  surface-variant: '#2f372e'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.08em
  support-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-padding: 1.25rem
  stack-gap-sm: 0.5rem
  stack-gap-md: 1rem
  stack-gap-lg: 1.5rem
  section-margin: 2rem
---

## Brand & Style

The design system is engineered for a high-performance utility application, balancing technical precision with effortless legibility. The aesthetic is **Modern Corporate** with a strong emphasis on **Functional Minimalism**. It aims to evoke a sense of reliability and real-time responsiveness.

Key characteristics include:
- **High-Contrast Utility:** Prioritizing rapid information retrieval through bold typography and stark tonal shifts.
- **Systematic Clarity:** Using a structured grid and consistent iconography to guide the user through complex data sets.
- **Action-Oriented Accents:** Utilizing a vibrant, saturated palette for state changes and active tracking to differentiate between "monitoring" and "recording" modes.

## Colors

The color strategy uses a light-gray foundation for the overall application frame to reduce glare while maintaining high contrast. 

- **Primary Green (#22C55E):** Used strictly for "Active" or "Running" states, indicators, and success confirmations.
- **Secondary Purple (#A855F7):** Reserved for toggles and specific interactive triggers to provide a clear visual departure from the status-driven green.
- **Deep Forest (#064E3B):** A specialized surface color for "Hero" status cards, providing a dark-mode container that anchors the main dashboard.
- **Neutral Scale:** Uses a precise range of cool grays to differentiate between primary labels (Black) and metadata/support text (Gray-500).

## Typography

The system utilizes **Hanken Grotesk** for its technical yet approachable grotesque qualities. 

- **Weighting:** Heavy weights (700-800) are used for screen titles and primary status numbers to ensure they are the first things a user sees.
- **Numerical Data:** Tabular figures are preferred for time and distance tracking to prevent "jumping" layouts during active updates.
- **Hierarchy:** Secondary metadata (like "duration" or "distance" labels under numbers) uses a smaller, lighter weight with increased line height to maintain a clean appearance.

## Layout & Spacing

This design system utilizes a **Fluid Grid** model optimized for mobile-first interactions.

- **Safe Zones:** A 20px (1.25rem) horizontal margin is maintained globally for all container elements.
- **Card Spacing:** Lists of recorded sessions use a 16px vertical gap.
- **Map Overlays:** The primary tracking interface uses a bottom-anchored persistent card. This card sits at a 16px margin from the bottom navigation and side edges to feel "floating" yet docked.
- **Information Density:** Settings and lists use a generous vertical height (min 72px for rows) to ensure high touch-accuracy during movement.

## Elevation & Depth

Depth is primarily communicated through **Tonal Layers** rather than heavy shadows.

- **Base Layer:** The global background is a very light gray (#F8FAF9).
- **Surface Layer:** White cards (#FFFFFF) sit on top of the base with a subtle, 2px stroke or a very soft, high-blur shadow (8% opacity) to provide separation.
- **Hero Elevation:** The Status Card uses a high-contrast dark background (#064E3B) to visually "pop" forward from the light background.
- **Interactive Elements:** Buttons and toggles use color fills rather than elevation to signify state.

## Shapes

The shape language is consistently **Rounded**, creating a modern and friendly feel for a technical tool.

- **Main Containers:** Large cards and the primary status overlay use a 1.5rem (`rounded-xl`) corner radius.
- **Interactive Components:** Toggles, secondary buttons, and icon containers use a 0.5rem (`rounded-md`) to 1rem (`rounded-lg`) radius.
- **Status Indicators:** Small "Live" or "Active" chips utilize a fully rounded pill shape (999px) for immediate recognition as a status tag.

## Components

### Status Card (Hero)
The primary dashboard element. It features a dark forest green background with high-contrast white text. Integrated controls (like the Play/Pause button) are placed on the far right for thumb accessibility.

### SettingsRows
Standardized rows for navigation and configuration.
- **Left:** Leading icon in a soft-gray square container.
- **Center:** Bold title with support description text below.
- **Right:** Trailing value (gray text) and a chevron-right icon.
- **Divider:** 1px hairline stroke between grouped items.

### List Items (Sessions)
Cards that encapsulate trip data. They use a vertical "Route Line" on the left margin (Primary Green) to visually link the session to the concept of a path. 

### Map Interface
The map uses a minimalist, light-themed tile provider. Interactive controls (Recenter, Layers) are grouped as floating circular white buttons on the right side of the screen.

### Bottom Navigation
A persistent white bar with a subtle top border. The active state is indicated by a soft green pill-shaped background behind the icon and bolded text labels.