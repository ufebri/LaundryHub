# LaundryHub UI Mirroring (High-Fidelity)

This project provides a technical reconstruction of the LaundryHub Android UI in Stitch, derived directly from source code analysis.

## Stitch Project Details
- **Project Name:** LaundryHub Pro
- **Project ID:** `14023573624442900648`
- **Design System Asset:** `assets/3e4232ff7c4f4fb8a5a3d7f4be3c70ab` (Synced from `DESIGN.md`)

## Accurate Screen Mapping
| Screen Name | Stitch Screen ID | Technical Specification |
|-------------|------------------|-------------------------|
| Onboarding / Login | `6b654b2f440a49948a7e9eed90736fd5` | Horizontal Pager + Lottie + Login with Google button. |
| Home Screen | `d21721e4d1cb43189ae204147dad3871` | Overlapping Header (90dp offset), Summary Grid, Pending Order Grid. |
| Inventory Screen | `7b8da6041a0044e09a022512e7f250a8` | Package Management header, 20dp rounded cards, Suggestion chips. |
| Transaction History | `fbe394496bd346da8dc62271e004736c` | Grouped date headers, Status badges, Rounded search bar. |
| Profile Screen | `c700a9dfebbf4ab0971ff2d11439fa29` | Dark themed cards (#31284B / #43365F) against light background. |
| New Order Bottom Sheet | `888a65f2b1b94e3b819c086c532f75e9` | 24px top-rounded sheet, Scrim overlay, Chip selectors. |

## Key Implementation Notes (Mirroring)
1. **Layout Logic:** The "Stacked Card" philosophy is maintained, ensuring vertical gaps and elevation match the Compose implementation.
2. **Color Weighting:** The Profile section uses the specific "Dark Surface" tokens identified in the theme code to provide visual variety.
3. **Hierarchy:** InfoCards on the dashboard correctly overlap the greeting header, providing the signature layered aesthetic of LaundryHub.
4. **Form Controls:** Order entry uses specific input patterns (Horizontal scrollable cards for packages) rather than generic dropdowns.

## Verification
- [x] Code Analysis (Phase 0-2)
- [x] DESIGN.md Specification
- [x] Design System Lock
- [x] High-Fidelity Generation
- [x] Documentation Handoff
