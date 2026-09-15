"""Generate ALEA launcher/splash/welcome icons from web logo.png."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "web" / "public" / "img" / "logo.png"
RES = ROOT / "softphone-v2" / "android" / "app" / "src" / "main" / "res"

BG = (10, 14, 23, 255)  # #0a0e17
ACCENT = (6, 182, 212, 255)  # #06b6d4

FG_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}
LEGACY = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def fit_on_canvas(src: Image.Image, size: int, pad_ratio: float = 0.12, bg=None) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), bg if bg is not None else (0, 0, 0, 0))
    inner = int(size * (1 - 2 * pad_ratio))
    im = src.copy()
    im.thumbnail((inner, inner), Image.Resampling.LANCZOS)
    x = (size - im.width) // 2
    y = (size - im.height) // 2
    canvas.paste(im, (x, y), im)
    return canvas


def round_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask


def circularize(icon: Image.Image) -> Image.Image:
    size = icon.size[0]
    alpha = round_mask(size)
    r, g, b, a = icon.split()
    return Image.merge("RGBA", (r, g, b, Image.composite(a, Image.new("L", (size, size), 0), alpha)))


def main() -> None:
    logo = Image.open(SRC).convert("RGBA")

    for folder, size in FG_SIZES.items():
        out = RES / folder / "linphone_launcher_icon_foreground.png"
        fit_on_canvas(logo, size, pad_ratio=0.08, bg=(0, 0, 0, 0)).save(out, "PNG")
        print("fg", out)

    for folder, size in LEGACY.items():
        icon = fit_on_canvas(logo, size, pad_ratio=0.06, bg=BG)
        (RES / folder / "ic_launcher.png").save if False else None
        icon.save(RES / folder / "ic_launcher.png", "PNG")
        circularize(icon).save(RES / folder / "ic_launcher_round.png", "PNG")
        print("legacy", folder, size)

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(exist_ok=True)
    fit_on_canvas(logo, 288, pad_ratio=0.05, bg=(0, 0, 0, 0)).save(nodpi / "alea_splash_icon.png", "PNG")
    fit_on_canvas(logo, 512, pad_ratio=0.02, bg=(0, 0, 0, 0)).save(nodpi / "alea_welcome_logo.png", "PNG")

    brand_w, brand_h = 400, 80
    brand = Image.new("RGBA", (brand_w, brand_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(brand)
    font = None
    for fp in (
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\segoeui.ttf",
    ):
        if Path(fp).exists():
            font = ImageFont.truetype(fp, 48)
            break
    if font is None:
        font = ImageFont.load_default()
    text = "ALEA"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(((brand_w - tw) / 2, (brand_h - th) / 2 - 4), text, font=font, fill=ACCENT)
    brand.save(RES / "drawable" / "linphone_splashscreen_branding.png", "PNG")
    print("branding + splash + welcome ok")


if __name__ == "__main__":
    main()
