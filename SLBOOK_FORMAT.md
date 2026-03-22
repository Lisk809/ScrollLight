# .slbook 文件格式规范

光言圣经（ScrollLight）专用圣经内容包格式，版本 1.0

## 概述

`.slbook` 是一个 **ZIP 压缩包**，内含 JSON/JSONL 文件，可包含三类圣经内容：
- **译本正文**（BIBLE_TEXT）
- **原文**（ORIGINAL_TEXT，希伯来文/希腊文）
- **研读本/注释**（STUDY_BIBLE / COMMENTARY）

三类书通过统一的寻址键 `bookId:chapter:verse` 相互链接。

---

## 文件结构

```
my_bible.slbook  (ZIP压缩包)
├── metadata.json        必须
├── verses.jsonl         译本/研读本必须；原文可选
├── words.jsonl          原文必须
├── notes.jsonl          研读本/注释必须
└── strongs.jsonl        词典可选（仅原文书或词典类书）
```

---

## 1. metadata.json

```json
{
  "bookId":      "cuv",
  "title":       "圣经·和合本",
  "abbr":        "和合本",
  "type":        "BIBLE_TEXT",
  "language":    "zh-CN",
  "testament":   "both",
  "description": "1919年出版，最广泛使用的中文圣经译本",
  "copyright":   "公共领域",
  "publisher":   "中国圣书公会",
  "version":     1,
  "hasOriginals": false,
  "hasNotes":    false,
  "checksum":    "sha256sum_of_this_file"
}
```

### type 可选值
| 值 | 说明 |
|----|------|
| `BIBLE_TEXT` | 圣经译本正文 |
| `ORIGINAL_TEXT` | 希伯来文/希腊文原文 |
| `STUDY_BIBLE` | 含注释的研读本 |
| `COMMENTARY` | 独立注释书 |
| `LEXICON` | 词汇词典 |

### language 常用值
`zh-CN` `zh-TW` `en` `he` `el` `ko` `ja` `es` `fr` `de`

### testament 可选值
`ot`（旧约）| `nt`（新约）| `both`（全本）

---

## 2. verses.jsonl（经文，每行一个节）

**字段说明：**
| 字段 | 类型 | 说明 |
|------|------|------|
| `b` | string | 书卷ID（见下方书卷ID表）|
| `c` | int | 章 |
| `v` | int | 节 |
| `t` | string | 经文文本 |
| `t2` | string | 可选辅助文本（如注释版的平行译文）|

```jsonl
{"b":"gen","c":1,"v":1,"t":"起初，神创造天地。"}
{"b":"gen","c":1,"v":2,"t":"地是空虚混沌，渊面黑暗；神的灵运行在水面上。"}
{"b":"mat","c":17,"v":1,"t":"过了六天，耶稣带着彼得、雅各，和雅各的兄弟约翰，暗暗地上了高山，"}
```

---

## 3. words.jsonl（原文逐字，每行一个词）

| 字段 | 类型 | 说明 |
|------|------|------|
| `b` | string | 书卷ID |
| `c` | int | 章 |
| `v` | int | 节 |
| `i` | int | 词在句中的序号（从0开始）|
| `s` | string | 原文字符（希伯来文/希腊文）|
| `l` | string | 词根形式（lemma）|
| `st` | string | Strong 编号（如 `H1234` 或 `G5678`）|
| `m` | string | 形态编码（如 `Ncmsa` = 名词-普通-男性-单数-宾格）|
| `tr` | string | 音译（拼音风格，如 `bereshit`）|
| `g` | string | 中文简短释义 |

```jsonl
{"b":"gen","c":1,"v":1,"i":0,"s":"בְּרֵאשִׁית","l":"רֵאשִׁית","st":"H7225","m":"Ncfsa","tr":"bereshit","g":"起初/开始"}
{"b":"gen","c":1,"v":1,"i":1,"s":"בָּרָא","l":"בָּרָא","st":"H1254","m":"Vqp3ms","tr":"bara","g":"创造"}
{"b":"mat","c":17,"v":1,"i":0,"s":"Καὶ","l":"καί","st":"G2532","m":"CC","tr":"kai","g":"和/又"}
```

---

## 4. notes.jsonl（注释，每行一条）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 全局唯一ID（建议格式：`{bookId}:{bookId}:{cf}:{vf}`）|
| `b` | string | 书卷ID |
| `cf` | int | 范围起始章 |
| `vf` | int | 范围起始节 |
| `ct` | int | 范围结束章（同章时等于cf）|
| `vt` | int | 范围结束节（单节时等于vf）|
| `title` | string | 注释标题（可空）|
| `content` | string | 正文（支持 Markdown）|
| `type` | string | 类型（见下）|
| `tags` | string | JSON数组字符串，如 `["神学","基督论"]` |

```jsonl
{"id":"tsb:mat:17:1","b":"mat","cf":17,"vf":1,"ct":17,"vt":8,"title":"登山变像","content":"**登山变像**是耶稣神性彰显的关键时刻...\n\n此事件发生在...","type":"VERSE_NOTE","tags":"[\"基督论\",\"荣耀\"]"}
{"id":"tsb:mat:17:0","b":"mat","cf":17,"vf":0,"ct":17,"vt":0,"title":"马太福音第17章引言","content":"本章包含三个重要事件：登山变像、赶鬼医病、以及殿税的问题...","type":"INTRODUCTION","tags":"[]"}
```

### type 可选值
| 值 | 说明 |
|----|------|
| `INTRODUCTION` | 书卷/章节引言（verse=0 表示章引言）|
| `VERSE_NOTE` | 节注（最常见）|
| `CROSS_REF` | 交叉参考 |
| `WORD_STUDY` | 原文词汇研究 |
| `THEME_NOTE` | 主题性注释 |

---

## 5. strongs.jsonl（Strong词典，可选）

```jsonl
{"id":"H7225","orig":"רֵאשִׁית","tr":"reshit","def":"开始，起初，首要，第一","defEn":"beginning, first, chief","usage":"用于时间的开始或系列的第一个","lang":"he"}
{"id":"G26","orig":"ἀγάπη","tr":"agape","def":"爱，神圣之爱","defEn":"love, affection","usage":"神对人的爱，以及基督徒之间的爱","lang":"el"}
```

---

## 书卷 ID 对照表

### 旧约 39卷
```
gen exo lev num deu jos jdg rut 1sa 2sa 1ki 2ki 1ch 2ch ezr neh est
job psa pro ecc sng isa jer lam ezk dan hos jol amo oba jon mic nam
hab zep hag zec mal
```

### 新约 27卷
```
mat mrk luk jhn act rom 1co 2co gal eph php col 1th 2th 1ti 2ti tit
phm heb jas 1pe 2pe 1jn 2jn 3jn jud rev
```

---

## 制作示例（Python）

```python
import json, zipfile

# 制作一个简单的测试译本
metadata = {
    "bookId": "my_version", "title": "我的测试译本", "abbr": "测试",
    "type": "BIBLE_TEXT", "language": "zh-CN", "testament": "nt",
    "description": "测试用途", "copyright": "自制", "version": 1,
    "hasOriginals": False, "hasNotes": False, "checksum": ""
}

verses = [
    {"b": "mat", "c": 1, "v": 1, "t": "亚伯拉罕的后裔，大卫的子孙，耶稣基督的家谱："},
    {"b": "mat", "c": 1, "v": 2, "t": "亚伯拉罕生以撒；以撒生雅各；雅各生犹大和他的兄弟们；"},
]

with zipfile.ZipFile("my_version.slbook", "w", zipfile.ZIP_DEFLATED) as zf:
    zf.writestr("metadata.json", json.dumps(metadata, ensure_ascii=False, indent=2))
    zf.writestr("verses.jsonl", "\n".join(json.dumps(v, ensure_ascii=False) for v in verses))

print("my_version.slbook created!")
```

---

## 版本更新

增量更新：提高 `metadata.json` 中的 `version` 值，应用会检测版本差异并提示更新。

---

## 三类书相互链接

所有三类书共享同一组 `bookId:chapter:verse` 地址空间：

```
用户阅读 mat:17:1
    ↓
主译本 (cuv)  mat:17:1 → "过了六天，耶稣带着..."
平行译本(niv)  mat:17:1 → "After six days Jesus took..."
原文 (na28)   mat:17:1 → [Καί, μεθ', ἡμέρας, ἕξ, ...]
研读本(tsb)   mat:17:1~8 → "登山变像..." (VERSE_NOTE)
```
