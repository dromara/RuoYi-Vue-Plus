---
name: GitHub Stars Library
colors:
  surface: '#fcf8f9'
  surface-dim: '#dcd9d9'
  surface-bright: '#fcf8f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3f3'
  surface-container: '#f0eded'
  surface-container-high: '#ebe7e7'
  surface-container-highest: '#e5e2e2'
  on-surface: '#1c1b1c'
  on-surface-variant: '#45474b'
  inverse-surface: '#313031'
  inverse-on-surface: '#f3f0f0'
  outline: '#76777b'
  outline-variant: '#c6c6cb'
  surface-tint: '#5b5e66'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#181c22'
  on-primary-container: '#80848c'
  inverse-primary: '#c3c6cf'
  secondary: '#585f68'
  on-secondary: '#ffffff'
  secondary-container: '#dce3ee'
  on-secondary-container: '#5e656e'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#231a12'
  on-tertiary-container: '#918176'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dfe2eb'
  primary-fixed-dim: '#c3c6cf'
  on-primary-fixed: '#181c22'
  on-primary-fixed-variant: '#43474e'
  secondary-fixed: '#dce3ee'
  secondary-fixed-dim: '#c0c7d2'
  on-secondary-fixed: '#151c24'
  on-secondary-fixed-variant: '#404750'
  tertiary-fixed: '#f3dfd2'
  tertiary-fixed-dim: '#d6c3b7'
  on-tertiary-fixed: '#231a12'
  on-tertiary-fixed-variant: '#51443b'
  background: '#fcf8f9'
  on-background: '#1c1b1c'
  surface-variant: '#e5e2e2'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: '1'
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: '1'
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  container-max: 1280px
  gutter: 16px
  sidebar-width: 260px
---

## Brand & Style
The design system is engineered for the modern developer: productive, intelligent, and hyper-efficient. It facilitates the discovery and organization of technical knowledge without visual friction. 

The aesthetic is **Corporate / Modern** with a focus on **Systematic Functionalism**. It draws heavily from developer-centric tools, prioritizing high information density and structural clarity over decorative elements. The interface should feel like an extension of a high-end IDE or a technical documentation site—reliable, fast, and utilitarian.

## Colors
The palette is rooted in the "GitHub Native" aesthetic to ensure immediate familiarity for developers. 

- **Primary & Action:** `#0d1117` provides deep, high-contrast text and structural elements, while `#0969da` is reserved strictly for interactive elements, links, and primary calls to action.
- **Surface Strategy:** Use `#f6f8fa` for the page background to reduce eye strain, and pure white (`#ffffff`) for cards and content containers to create a clear visual stack.
- **Borders:** Subtle `#d0d7de` borders are the primary method of separation, replacing heavy shadows for a flatter, more professional appearance.
- **Semantic Logic:** Success, Warning, and Info colors follow standard technical conventions to indicate task status and system health without ambiguity.

## Typography
This design system utilizes **Inter** for all UI and prose content to ensure maximum legibility across all display densities. For technical strings, repository names, and tags, **JetBrains Mono** is introduced to provide a distinct "code-like" feel that helps developers quickly scan technical metadata.

- **Contrast:** Maintain a strict hierarchy. Headings should be semi-bold with tighter letter spacing to feel "tight" and professional.
- **Scale:** Body text defaults to 14px for high-density information layouts, scaling up to 16px for long-form descriptions or blog-style content.

## Layout & Spacing
The layout follows a **Fixed Grid** philosophy for desktop to maintain readability of repository lists, while transitioning to a **Fluid** model for mobile devices.

- **Desktop:** Features a persistent left sidebar (260px) for navigation and category filtering. The main content area utilizes a 12-column grid with a 1280px max-width.
- **Mobile (H5):** Content utilizes 16px side margins. Navigation is moved to a bottom tab bar to optimize for one-handed reachability.
- **Rhythm:** An 8px-based spacing system ensures consistent alignment. Use `16px` for standard gutters between cards and `24px` for major section padding.

## Elevation & Depth
In this design system, depth is communicated through **Tonal Layers** and **Low-contrast Outlines** rather than dramatic shadows.

- **Base Layer:** `#f6f8fa` (Background).
- **Content Layer:** Pure white cards with a `1px` solid border in `#d0d7de`. 
- **Interactive Elevation:** On hover, cards may transition to a very subtle ambient shadow (4px blur, 0.05 opacity) to indicate interactivity, but the primary indicator remains the border or title color change.
- **Overlays:** Modals and dropdowns use a slightly more pronounced shadow to separate them from the content layers, ensuring focus on the active task.

## Shapes
The shape language is **Soft** yet disciplined. 

- **Standard Elements:** Buttons, input fields, and cards use a `0.25rem` (4px) corner radius. This maintains a precise, technical feel without the harshness of sharp corners.
- **Large Elements:** Container-level components like modals or large feature cards use a `0.5rem` (8px) radius to feel more approachable.
- **Tags/Badges:** Use a more aggressive `2rem` radius to create a "pill" shape, clearly differentiating metadata from structural UI components.

## Components
- **Buttons:** 
  - *Primary:* Solid `#0969da` with white text. 
  - *Secondary:* White background with `#d0d7de` border and `#24292f` text.
- **Cards:** White surfaces, `1px` border in `#d0d7de`. Title links use Action Blue on hover.
- **Tag/Badge System:** Use JetBrains Mono for text. Backgrounds are soft versions of semantic colors (e.g., Success Green at 10% opacity) with the text in the full-saturation color for accessibility.
- **Input Fields:** Flat design, white background, `1px` border. On focus, the border changes to Action Blue with a subtle blue outer glow (ring).
- **Progress Indicators:** Use a linear bar for AI/Import tasks. Use Action Blue for the fill, with a `#f0f0f0` track. For indeterminate states, use a subtle pulse animation.
- **Navigation:** 
  - *Sidebar (PC):* Vertical list with clear active states (left-side blue indicator bar).
  - *Bottom Tab (H5):* Icons with labels, high contrast between active and inactive states using Action Blue.