# Tally — Digital Assets (Brand & Play Store Kit)

Store-listing and brand assets, built from the app's real launcher mark: four brand-green tally
strokes crossed by one gold diagonal (the "5"). Everything here is vector (SVG) master art, so it
scales cleanly and stays editable. Colors and geometry match `ic_launcher_foreground.xml` /
`ic_launcher_background.xml` exactly.

## Files

| File | Size | Purpose |
|------|------|---------|
| `app_icon_512.svg` | 512×512 | Play Store app icon — white field (matches the on-device icon). |
| `app_icon_512_green.svg` | 512×512 | App icon, green-plate variant — punchier on the store. **Recommended for the listing.** |
| `feature_graphic_1024x500.svg` | 1024×500 | Google Play feature graphic (header banner). |
| `adaptive_foreground_512.svg` | 512×512 | Adaptive-icon foreground layer (transparent, inside the safe zone). |
| `adaptive_background_512.svg` | 512×512 | Adaptive-icon background layer (solid white). |
| `logo_mark.svg` | 512×512 | The mark alone, full color, transparent — use anywhere it stands alone. |
| `logo_monochrome.svg` | 512×512 | Single-color mark (uses `currentColor`) for one-color / dark contexts. |
| `logo_lockup.svg` | 1080×360 | Horizontal lockup: mark + "Tally" wordmark. |

## Brand palette

| Token | Hex | Where |
|-------|-----|-------|
| Brand Green (primary / strokes) | `#1B7A46` | tally bars, primary buttons/FAB |
| Gold Accent (the diagonal) | `#E3A73B` | logo slash, Catan/streak accents |
| Mint (dark-mode primary) | `#58D690` | dark-theme primary, tagline |
| Cream Canvas | `#F4F5EF` | light app background |
| Dark Forest | `#111A15` | dark app background |
| Ink (primary text) | `#1A1B18` | wordmark, titles |
| Green Container | `#D3EFDC` | secondary/pastel buttons |

## ⚠️ Before you upload — convert to raster

Google Play does **not** accept SVG. Export these masters to raster first:
- **App icon** → 512×512 **32-bit PNG** (with alpha).
- **Feature graphic** → 1024×500 **PNG (24-bit) or JPEG** (no alpha).

Export with Android Studio (Vector Asset / right-click → export), Inkscape, or any SVG→PNG tool.
Keep the SVGs here as the editable source of truth.

## ⚠️ Wordmark font

`logo_lockup.svg` and `feature_graphic_1024x500.svg` set the "Tally" wordmark in **Plus Jakarta Sans**
(the app's typeface) with a system fallback. On a machine without that font it renders in the
fallback. Before publishing, either install Plus Jakarta Sans or **convert the `<text>` to outlines**
(Inkscape: Path → Object to Path) so the wordmark looks identical everywhere.

## Notes

- Zero shadows / gradients / blur — flat by design, matching the app's visual language. (The feature
  graphic's large background mark is flat low-opacity, not a blur.)
- The adaptive foreground keeps all strokes inside the ~66% safe zone, so launcher masks (circle,
  squircle, rounded-square) never crop the mark.
