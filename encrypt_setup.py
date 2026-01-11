import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

def derive_key():
    package_name = "com.suvojeet.suvmusic"
    # Logic: replace(".", "").reversed().take(16).padEnd(16, 'S')
    base = package_name.replace(".", "")
    reversed_base = base[::-1]
    key = reversed_base[:16]
    key = key.ljust(16, 'S')
    return key

def encrypt(plain_text, key):
    # AES/CBC/PKCS5Padding (PKCS7 is same as PKCS5 for AES)
    key_bytes = key.encode('utf-8')
    cipher = AES.new(key_bytes, AES.MODE_CBC)
    ct_bytes = cipher.encrypt(pad(plain_text.encode('utf-8'), AES.block_size))
    
    # Combine IV + ciphertext
    combined = cipher.iv + ct_bytes
    return base64.b64encode(combined).decode('utf-8')

key = derive_key()
print(f"Key: {key}")

url = "https://www.jiosaavn.com/api.php"
password = "01082005"

print(f"ENC_BASE_URL: {encrypt(url, key)}")
print(f"ENC_DEV_PASSWORD: {encrypt(password, key)}")
