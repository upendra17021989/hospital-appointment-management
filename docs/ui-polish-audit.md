# UI Polish Audit

## Scope

This audit establishes the baseline for a systematic visual and interaction polish. It is based on the supplied Appointments screenshot and inspection of the React components and SCSS source. Live browser breakpoint verification remains required when a browser session is available.

Representative workflows:

- Dashboard and navigation shell
- Appointments and appointment booking
- Patients and patient details
- Prescriptions and medical certificates
- Reports, billing, and receipts
- Doctors, departments, users, and hospital settings

Target viewport checks:

- Desktop: 1440px and wider
- Laptop: 1024px to 1439px
- Tablet: 768px to 1023px
- Mobile: 360px to 767px

## What Should Stay

- Warm neutral background and card palette
- Terracotta primary color and green clinical-success color
- Playfair Display headings paired with DM Sans body text
- Rounded cards and restrained shadows
- Dark sidebar and clear active navigation treatment
- Existing status colors and compact token badges

The product needs consistency and interaction refinement, not a visual rebrand.

## Baseline Findings

### 1. Design tokens are incomplete and duplicated

- `_variables.scss` declares the main color palette twice.
- Only one responsive breakpoint (`768px`) exists, so tablet and smaller-laptop layouts have no dedicated behavior.
- Spacing, control heights, z-index layers, motion, focus rings, and content widths are not tokenized.
- Some runtime CSS custom properties exist, but several semantic colors are available only as Sass variables.

### 2. Reusable components exist but are not used consistently

- `PageHeader`, `Badge`, `LoadingSpinner`, `EmptyState`, `Modal`, and `Tabs` already exist.
- Several pages recreate page headers, controls, action groups, and tables locally.
- Pages mix reusable components with one-off markup, producing different spacing and responsive behavior.
- `Modal` lacks dialog semantics, focus trapping, Escape handling, and an accessible close label.

### 3. Inline styling is the largest source of visual drift

There are approximately 420 `style={{...}}` blocks in the frontend.

Highest concentrations:

| Page | Inline style blocks |
| --- | ---: |
| Book Appointment | 61 |
| Prescription Form | 39 |
| Appointments | 33 |
| Doctor Management | 26 |
| Patients | 24 |
| Billing Receipt Viewer | 21 |
| Patient Details | 20 |

These should be migrated incrementally into page-level classes and shared primitives. A wholesale mechanical rewrite is not recommended.

### 4. Responsive behavior jumps directly from desktop to mobile

- Most layout rules switch only at `768px`.
- Dense tables and action groups become compressed on laptops and tablets before mobile cards activate.
- Page-header controls may compete with titles at intermediate widths.
- Filters, tabs, modal forms, and multi-button actions need explicit wrapping and overflow rules.

### 5. Tables and actions are inconsistent

- Tables use global element selectors plus page-specific overrides.
- Column widths, numeric alignment, empty states, sorting affordances, and responsive overflow vary by page.
- Destructive and secondary row actions are often displayed alongside the primary action, increasing density.
- The supplied Appointments screenshot demonstrated token/date wrapping and separated action groups; the immediate alignment issue has been fixed, but the broader table pattern remains inconsistent.

### 6. User feedback is fragmented

- The frontend contains 19 native `alert()` or `confirm()` calls.
- Other pages use inline alerts or silently swallow errors.
- Success, failure, loading, and destructive confirmation behavior differs by workflow.
- Duplicate submissions are not prevented consistently.

### 7. Accessibility needs a dedicated pass

- Focus styles are not defined as a coherent system.
- Clickable cards are frequently `div` elements rather than buttons or links.
- Modal focus behavior and keyboard dismissal are incomplete.
- Icon-only controls need accessible names.
- Status badges require text and cannot depend on color alone.
- Motion should respect `prefers-reduced-motion`.

### 8. Source encoding artifacts are present

Several source files contain malformed symbols for arrows, checkmarks, multiplication signs, currency marks, and emoji. These should be replaced with UTF-8 text or shared SVG icons during page polish.

## Implementation Priorities

### Foundation

1. Normalize and expand design tokens.
2. Add laptop, tablet, and mobile breakpoint helpers.
3. Establish focus, disabled, loading, and reduced-motion behavior.
4. Define page width, section spacing, and action-layout utilities.

### Shared primitives

1. Strengthen `PageHeader`, `Tabs`, `Modal`, `EmptyState`, and `LoadingSpinner`.
2. Add `FilterBar`, `FormField`, `DataTable`, `Pagination`, `ActionMenu`, `ConfirmDialog`, and `Toast`.
3. Document intended variants and accessibility requirements.

### Reference workflows

Polish Appointments and Book Appointment first. They cover tables, filters, tabs, forms, steps, validation, modals, destructive actions, and responsive behavior. Their resulting patterns should be reused elsewhere.

## Acceptance Criteria

- No duplicate token declarations.
- Shared viewport behavior exists for desktop, laptop, tablet, and mobile.
- All interactive elements have visible keyboard focus.
- Appointments and Booking contain no page-specific layout inline styles after their polish phase, except values that are genuinely data-driven.
- Dense desktop tables scroll safely before switching to mobile cards.
- Primary, secondary, and destructive actions have consistent hierarchy.
- Native `alert()` and `confirm()` calls reach zero after the feedback phase.
- Shared modal and confirmation components support keyboard operation and focus management.
- No malformed UI symbols remain in polished pages.
- Frontend lint, unit tests, and production build pass after every page group.

## Verification Still Required

When a browser is connected, capture and review each representative workflow at 1440px, 1280px, 1024px, 768px, and 390px. Record screenshots before and after each page-group phase to detect visual regressions.
