import os
from PIL import Image, ImageOps

def generate_icons(source_path, res_path):
    if not os.path.exists(source_path):
        print(f"Error: Source file not found at {source_path}")
        return

    try:
        img = Image.open(source_path).convert("RGBA")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    # Densities and sizes for legacy icons (standard launcher)
    # mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    # 1. Generate Legacy Icons (square and round)
    # Note: Modern 'round' icons are usually circular crops.
    for folder, size in densities.items():
        out_folder = os.path.join(res_path, folder)
        os.makedirs(out_folder, exist_ok=True)
        
        # Resize
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save square
        resized.save(os.path.join(out_folder, "ic_launcher.webp"), "WEBP", quality=90)
        
        # Create round version (simple circle mask)
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_icon = ImageOps.fit(resized, mask.size, centering=(0.5, 0.5))
        round_icon.putalpha(mask)
        round_icon.save(os.path.join(out_folder, "ic_launcher_round.webp"), "WEBP", quality=90)
        print(f"Generated legacy icons for {folder}")

    # 2. Generate Adaptive Foreground
    # For adaptive icons, the foreground should be 108dp. 
    # xxxhdpi (4x) is 432x432px. We'll use this as a high-res drawable.
    # We save it to 'drawable' so it can be used by the adaptive-icon xml.
    # Note: detailed masking happens by the OS. We just provide the full image (or centered logo).
    foreground_size = 432
    foreground_img = img.resize((foreground_size, foreground_size), Image.Resampling.LANCZOS)
    
    drawable_folder = os.path.join(res_path, "drawable")
    os.makedirs(drawable_folder, exist_ok=True)
    foreground_path = os.path.join(drawable_folder, "ic_launcher_foreground.webp")
    foreground_img.save(foreground_path, "WEBP", quality=90)
    print(f"Generated adaptive foreground at {foreground_path}")

    # 3. Generate Generic Logo (for in-app usage)
    logo_path = os.path.join(drawable_folder, "logo.webp")
    # Resize to a reasonable size, e.g., 512x512
    logo_img = img.resize((512, 512), Image.Resampling.LANCZOS)
    logo_img.save(logo_path, "WEBP", quality=95)
    print(f"Generated generic logo at {logo_path}")

    # 4. Detect Background Color
    # Sample the top-left pixel
    bg_color = img.getpixel((0, 0))
    # Check if it's RGBA
    if len(bg_color) == 4:
         # if fully transparent, maybe default to white or black, but let's assume it's opaque for color extraction
         pass
    
    # Convert to hex
    hex_color = '#{:02x}{:02x}{:02x}'.format(bg_color[0], bg_color[1], bg_color[2])
    print(f"DETECTED_BACKGROUND_COLOR:{hex_color}")

if __name__ == "__main__":
    SOURCE = r"C:/Users/suvoj/.gemini/antigravity/brain/fc424e16-56ec-4240-b83b-86448ca0a403/uploaded_image_1767640205022.jpg"
    RES_DIR = r"f:\SuvMusic\app\src\main\res"
    generate_icons(SOURCE, RES_DIR)
