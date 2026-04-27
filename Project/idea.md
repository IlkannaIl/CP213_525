
#แก้ไอเดีย

* **คอนเซปต์หลัก (Core Concept)**
เปลี่ยนคีย์บอร์ดมาตรฐานให้เป็น AI Communication Assistant ที่ช่วยให้ผู้ใช้สื่อสารต่างภาษาได้อย่างมั่นใจในทุกแอปพลิเคชัน (Facebook, Email, Tinder, Slack) โดยไม่ต้องสลับหน้าจอไปมา เพื่อการสื่อสารที่ถูกต้องตามหลักไวยากรณ์และเหมาะสมกับวัฒนธรรม

* **ฟีเจอร์หลัก (Key Features)**
- Smart Refine (ปุ่มไม้กายสิทธิ์)
  - How it works: พิมพ์ภาษาไทยแบบกันเอง -> กดปุ่ม Refine -> คีย์บอร์ดเปลี่ยนข้อความนั้นเป็นภาษาต่างประเทศที่สละสลวยทันที

  - Tech: ส่งข้อความไปยัง AI (LLM) เพื่อปรับรูปประโยค

---

# 🚀 Gemini API Integration Project (Android/Mobile)

โปรเจกต์เชื่อมต่อ Google Gemini AI สำหรับการประมวลผลข้อความและมัลติมีเดีย โดยใช้โมเดลล่าสุดเวอร์ชันปี 2026

## 🛠 การตั้งค่าเริ่มต้น (Configuration)

### 1. API Key
- รับ API Key ได้ที่ [Google AI Studio](https://aistudio.google.com/)
- **คำเตือน:** ห้ามนำ API Key ไปเผยแพร่ใน GitHub Public Repo หรือในแชทสาธารณะ

### 2. Model Endpoint
ปัจจุบันโปรเจกต์นี้ใช้โมเดล **Gemini 2.5 Flash** เป็นหลัก เพื่อความรวดเร็วและประหยัดโควตา
- **Base URL:** `https://generativelanguage.googleapis.com/v1beta/`
- **Model Name:** `models/gemini-2.5-flash`

---

## 💻 วิธีการเรียกใช้งาน (API Usage)

### การส่งคำขอ (POST Request)
ใช้ Endpoint ต่อไปนี้สำหรับการส่งข้อความ:
`POST {Base URL}/models/gemini-2.5-flash:generateContent?key={YOUR_API_KEY}`

**Request Body (JSON):**
```json
{
  "contents": [{
    "parts": [{"text": "Hello, how are you today?"}]
  }]
}