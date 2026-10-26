#!/usr/bin/env bash
#
# Generates every launcher-icon asset of the app from the original AB IT logo
# (branding/ab_it_multimedia_app_icon.png). This is a scripted, reproducible
# equivalent of Android Studio's "Image Asset" wizard.
#
# Requires ImageMagick 7 (`brew install imagemagick`). Run from the project root:
#   ./branding/generate_launcher_icons.sh
#
# What it produces
#   mipmap-*/ic_launcher_foreground.webp  adaptive icon foreground (API 26+)
#   mipmap-*/ic_launcher_monochrome.webp  single-colour layer for themed icons (API 33+)
#   mipmap-*/ic_launcher.webp             legacy square icon (API 24-25)
#   mipmap-*/ic_launcher_round.webp       legacy round icon (API 24-25)
#   drawable-nodpi/ab_it_logo.webp        logo shown inside the app
#   src/main/ic_launcher-playstore.png    512 x 512 store icon
#
# The adaptive background is the vector drawable drawable/ic_launcher_background.xml,
# a radial gradient that uses the same two navy colours as BG_INNER / BG_OUTER below.
set -euo pipefail

SRC="branding/ab_it_multimedia_app_icon.png"
RES="app/src/main/res"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Measured from the 1254 x 1254 source logo:
#  - the navy rounded square spans x 73..1181 and y 65..1187 (centre 627,626)
#  - the furthest artwork pixel is 545 px from that centre
CENTER_X=627
CENTER_Y=626
ARTWORK_RADIUS_PX=545

# Adaptive icons are 108 dp; everything important must sit inside the central
# 66 dp circle (radius 33 dp). Mapping the artwork radius to 32.5 dp keeps a small
# margin, so 108 dp corresponds to 108 * 545 / 32.5 = 1811 source pixels.
CANVAS_PX=$(( 108 * ARTWORK_RADIUS_PX * 10 / 325 ))

# Navy colours of the logo background (brightest near the top-left, darker below).
BG_INNER="#02377E"
BG_OUTER="#021C4A"

# 1. Make the white area outside the rounded square transparent, then shave the
#    1-2 px light anti-aliasing rim so no white halo remains.
magick "$SRC" -alpha set -fuzz 12% -fill none \
    -draw "color 0,0 floodfill" -draw "color 1253,0 floodfill" \
    -draw "color 0,1253 floodfill" -draw "color 1253,1253 floodfill" \
    \( +clone -alpha extract -morphology Erode Disk:3 -blur 0x1 \) \
    -alpha off -compose CopyOpacity -composite "$WORK/logo_masked.png"

# 2. Centre the masked logo on the 108 dp canvas (still at source resolution).
OFFSET_X=$(( CANVAS_PX / 2 - CENTER_X ))
OFFSET_Y=$(( CANVAS_PX / 2 - CENTER_Y ))
magick -size "${CANVAS_PX}x${CANVAS_PX}" xc:none "$WORK/logo_masked.png" \
    -geometry "+${OFFSET_X}+${OFFSET_Y}" -compose Over -composite "$WORK/foreground_full.png"

# 2b. Monochrome layer for themed icons (Android 13+): the logo's white artwork plus the cyan
#     "IT" as one single-colour silhouette, placed on the same canvas as the foreground.
magick "$SRC" -colorspace Gray -threshold 80% "$WORK/mono_white.png"
magick "$SRC" -channel B -separate -threshold 70% "$WORK/mono_b.png"
magick "$SRC" -channel G -separate -threshold 45% "$WORK/mono_g.png"
magick "$SRC" -channel R -separate -threshold 30% -negate "$WORK/mono_r.png"
#     Only look for the cyan "IT" in the text band, so the blue ring and tiles are left out.
magick -size 1254x1254 xc:black -fill white -draw "rectangle 0,880 1253,1090" "$WORK/mono_text_band.png"
magick "$WORK/mono_b.png" "$WORK/mono_g.png" -compose Multiply -composite \
    "$WORK/mono_r.png" -compose Multiply -composite \
    "$WORK/mono_text_band.png" -compose Multiply -composite "$WORK/mono_cyan.png"
magick "$WORK/logo_masked.png" -alpha extract -morphology Erode Disk:12 "$WORK/mono_inside.png"
magick "$WORK/mono_white.png" "$WORK/mono_cyan.png" -compose Lighten -composite \
    "$WORK/mono_inside.png" -compose Multiply -composite -morphology Open Disk:1 "$WORK/mono_mask.png"
magick -size 1254x1254 xc:white "$WORK/mono_mask.png" -alpha off -compose CopyOpacity -composite \
    "$WORK/mono_logo.png"
magick -size "${CANVAS_PX}x${CANVAS_PX}" xc:none "$WORK/mono_logo.png" \
    -geometry "+${OFFSET_X}+${OFFSET_Y}" -compose Over -composite "$WORK/monochrome_full.png"

# 3. Background raster (only used to render the legacy round + Play Store icons).
#    Same gradient as drawable/ic_launcher_background.xml: centre (31.5dp, 25.6dp), radius 54dp.
BG_CX=$(( CANVAS_PX * 315 / 1080 ))
BG_CY=$(( CANVAS_PX * 256 / 1080 ))
BG_R=$(( CANVAS_PX * 54 / 108 ))
magick -size "${CANVAS_PX}x${CANVAS_PX}" \
    -define gradient:center="${BG_CX},${BG_CY}" -define gradient:radii="${BG_R},${BG_R}" \
    "radial-gradient:${BG_INNER}-${BG_OUTER}" "$WORK/background_full.png"
magick "$WORK/background_full.png" "$WORK/foreground_full.png" -compose Over -composite \
    "$WORK/composite_full.png"

# Only the central 72 dp of an adaptive icon is ever visible.
VIEWPORT_PX=$(( CANVAS_PX * 72 / 108 ))
magick "$WORK/composite_full.png" -gravity center -crop "${VIEWPORT_PX}x${VIEWPORT_PX}+0+0" +repage \
    "$WORK/viewport.png"

# Legacy square icon: the original rounded-square logo, trimmed to its own edge.
magick "$WORK/logo_masked.png" -trim +repage -background none -gravity center \
    -extent 1130x1130 "$WORK/legacy_square.png"

# Legacy round icon: the adaptive composite cut to a circle.
HALF=$(( VIEWPORT_PX / 2 ))
magick "$WORK/viewport.png" \( -size "${VIEWPORT_PX}x${VIEWPORT_PX}" xc:black -fill white \
    -draw "circle ${HALF},${HALF} ${HALF},1" \) \
    -alpha off -compose CopyOpacity -composite "$WORK/legacy_round.png"

webp() { magick "$1" -resize "$2x$2" -define webp:lossless=true -quality 100 "$3"; }

# density : legacy icon size (48 dp) : adaptive layer size (108 dp)
for spec in mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432; do
    IFS=: read -r density legacy adaptive <<< "$spec"
    dir="$RES/mipmap-$density"
    mkdir -p "$dir"
    webp "$WORK/foreground_full.png" "$adaptive" "$dir/ic_launcher_foreground.webp"
    webp "$WORK/monochrome_full.png" "$adaptive" "$dir/ic_launcher_monochrome.webp"
    # Legacy icons keep a 2 dp transparent margin like the platform templates.
    inner=$(( legacy * 44 / 48 ))
    for kind in square:ic_launcher round:ic_launcher_round; do
        IFS=: read -r shape name <<< "$kind"
        magick "$WORK/legacy_${shape}.png" -resize "${inner}x${inner}" -background none -gravity center \
            -extent "${legacy}x${legacy}" -define webp:lossless=true -quality 100 "$dir/$name.webp"
    done
done

# Logo shown in the drawer header and About screen (rounded square, transparent corners).
mkdir -p "$RES/drawable-nodpi"
webp "$WORK/legacy_square.png" 512 "$RES/drawable-nodpi/ab_it_logo.webp"

# 512 x 512 full-bleed store icon (Google Play applies its own mask).
magick "$WORK/viewport.png" -resize 512x512 -depth 8 -strip app/src/main/ic_launcher-playstore.png

# Previews for checking the result by eye (not used by the app).
if [[ "${1:-}" == "--preview" ]]; then
    PREVIEW_DIR="${2:-$WORK}"
    mkdir -p "$PREVIEW_DIR"
    magick "$WORK/legacy_round.png" -resize 384x384 "$PREVIEW_DIR/preview_circle.png"
    magick "$WORK/viewport.png" -resize 384x384 \( -size 384x384 xc:black -fill white \
        -draw "roundrectangle 0,0 383,383 96,96" \) -alpha off -compose CopyOpacity -composite \
        "$PREVIEW_DIR/preview_squircle.png"
    magick "$WORK/composite_full.png" -resize 432x432 -fill none -stroke red -strokewidth 2 \
        -draw "circle 216,216 216,84" -stroke yellow -draw "rectangle 72,72 360,360" \
        "$PREVIEW_DIR/preview_safe_zone.png"
    echo "Previews written to $PREVIEW_DIR"
fi

echo "Launcher icons generated from $SRC"
