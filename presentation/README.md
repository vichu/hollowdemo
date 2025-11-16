# Hollow Talk - ACM Fremont Chapter

**Versioned Datasets: Rethinking Local In-Memory Caches**

Presentation by Viswanathan Ranganathan (Vish)
ACM Fremont Chapter - Emerging Technologies Tech Talk
November 16, 2025

---

## 📊 View the Presentation

**Live Slides:** [GitHub Pages URL - TBD]

---

## 🚀 Run Locally

### Prerequisites
- Node.js 16+ installed

### Installation & Run

```bash
# Install dependencies
npm install

# Start the presentation
npm run dev
```

The slides will open automatically at `http://localhost:3030/`

**Presenter Mode:** `http://localhost:3030/presenter/` (shows speaker notes)

---

## 📁 Files

- **slides.md** - Main presentation (Slidev format)
- **TALK_OUTLINE.md** - Detailed outline with timing and structure
- **SPEAKER_NOTES.md** - Full speaker script and delivery notes
- **style.css** - Custom presentation styles

---

## 🎯 About the Talk

This talk explores Hollow, an open-source library by Netflix that enables efficient distribution and updating of multi-gigabyte datasets using a versioned, delta-based approach—essentially applying Git's model to in-memory data.

**Duration:** 20 minutes + 5 minutes Q&A

**Key Topics:**
- Operational challenges of local in-memory caches at GB scale
- Versioned dataset distribution pattern
- Live demo with real numbers (10 MB snapshots, 623 KB deltas)
- When to use (and when NOT to use) this pattern

---

## 🔗 Resources

- **Hollow Documentation:** [hollow.how](https://hollow.how)
- **Hollow GitHub:** [github.com/Netflix/hollow](https://github.com/Netflix/hollow)
- **Demo from this talk:** [github.com/vichu/hollowdemo](https://github.com/vichu/hollowdemo)

---

## 📦 Export Slides

```bash
# Export to PDF
npm run export

# Build for deployment
npm run build
```

---

## 📝 License

Presentation content: CC BY 4.0
Demo code: MIT License
