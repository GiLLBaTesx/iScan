# Kiro Configuration for iScan

This directory contains Kiro-specific configuration files that enhance development workflow.

## 📁 Directory Structure

```
.kiro/
├── steering/                    # Context & convention files
│   ├── android-conventions.md   # Always included
│   ├── security-standards.md    # Always included
│   ├── analytics-tracking.md    # Auto-included for *Screen.kt files
│   └── project-architecture.md  # Manual inclusion only
├── specs/                       # Spec session files (future)
└── README.md                    # This file
```

## 📚 Steering Files

Steering files provide additional context and conventions to Kiro during development. They ensure consistent code quality and adherence to project standards.

### 1. **android-conventions.md** (Always Active)
Automatically loaded for all interactions.

**Contains:**
- ✅ Azure Glass color palette (ElectricBlue, IcyCyan, etc.)
- ✅ Kotlin & Compose best practices
- ✅ File naming conventions
- ✅ MVVM architecture patterns
- ✅ Code style guidelines
- ✅ Git commit conventions

**Purpose:** Ensures all code follows consistent Android/Kotlin standards and always uses the correct Azure Glass theme colors.

### 2. **security-standards.md** (Always Active)
Automatically loaded for all interactions.

**Contains:**
- ✅ Database encryption requirements (SQLCipher)
- ✅ Authentication & authorization patterns
- ✅ Firebase security rules
- ✅ Network security (HTTPS enforcement)
- ✅ Secure logging (SecureLogger usage)
- ✅ Input validation patterns
- ✅ ProGuard/R8 configuration
- ✅ Privacy compliance (GDPR/COPPA)

**Purpose:** Guarantees all code follows security best practices and protects user data.

### 3. **analytics-tracking.md** (Auto-included for Screen files)
Automatically loaded when editing files matching `*Screen.kt`.

**Contains:**
- ✅ Screen tracking patterns
- ✅ Event tracking guidelines
- ✅ What to track / what NOT to track
- ✅ Performance tracking patterns
- ✅ Scan-specific tracking
- ✅ Privacy-safe analytics rules

**Purpose:** Ensures proper analytics implementation without tracking personal data.

### 4. **project-architecture.md** (Manual Only)
Only loaded when explicitly referenced with `#project-architecture`.

**Contains:**
- 📖 High-level architecture overview
- 📖 Directory structure details
- 📖 MVVM data flow diagrams
- 📖 Security architecture
- 📖 Analytics architecture
- 📖 Design patterns used
- 📖 Testing strategy

**Purpose:** Reference documentation for understanding the overall project structure.

**How to use:** Mention `#project-architecture` in your message to Kiro.

## 🎯 What Steering Does

### Automatic Enforcement:
1. **Color Consistency** - Kiro will always use Azure Glass theme colors
2. **Security Practices** - Kiro follows security standards automatically
3. **Code Style** - Kiro writes consistent Kotlin/Compose code
4. **Analytics** - Kiro implements privacy-safe tracking
5. **Naming** - Kiro uses correct file and variable names

### Developer Benefits:
- ✅ No need to remember color codes
- ✅ Security patterns applied automatically
- ✅ Consistent code style across files
- ✅ Proper analytics without PII leakage
- ✅ Faster code reviews (conventions followed)

## 🔧 How to Modify Steering

### Edit Existing Files:
```bash
# Edit Android conventions
code .kiro/steering/android-conventions.md

# Edit Security standards
code .kiro/steering/security-standards.md
```

### Add New Steering File:
```bash
# Create new file
touch .kiro/steering/my-custom-rules.md

# Add front-matter
cat > .kiro/steering/my-custom-rules.md << 'EOF'
---
inclusion: always
---

# My Custom Rules

Your rules here...
EOF
```

### Front-Matter Options:

```yaml
---
# Always included (default)
inclusion: always
---
```

```yaml
---
# Only included when specific files are edited
inclusion: fileMatch
fileMatchPattern: '*.kt'
---
```

```yaml
---
# Only included when explicitly referenced
inclusion: manual
---
```

## 📖 File References

You can reference other files in steering files:

```markdown
See the API spec: #[[file:docs/api-spec.yaml]]
Check the schema: #[[file:schema/database.sql]]
```

This includes those files in Kiro's context automatically.

## 🚀 Best Practices

### ✅ DO:
- Keep steering files focused and concise
- Update steering when patterns change
- Use manual inclusion for large reference docs
- Include code examples in steering files
- Version control steering files (already done)

### ❌ DON'T:
- Put sensitive data in steering files
- Make steering files too large (split them up)
- Duplicate information across files
- Include personal preferences (keep it team-wide)
- Forget to commit changes to steering

## 📊 Current Configuration Status

| Feature | Status |
|---------|--------|
| Android conventions | ✅ Active (always) |
| Security standards | ✅ Active (always) |
| Analytics tracking | ✅ Active (Screen files) |
| Project architecture | ✅ Available (manual) |
| Color enforcement | ✅ Automatic |
| Security enforcement | ✅ Automatic |

## 💡 Tips

### For Quick Edits:
Just edit files normally. Steering is active automatically.

### For Complex Changes:
Reference `#project-architecture` to get full context about the app structure.

### For New Features:
Steering will guide Kiro to use correct patterns, colors, and security practices automatically.

### For Analytics:
When editing Screen files, analytics tracking patterns are automatically included.

## 📚 Documentation

- **Kiro Documentation**: https://docs.kiro.dev
- **Steering Guide**: https://docs.kiro.dev/steering
- **Skills & Hooks**: https://docs.kiro.dev/customization

---

**Last Updated**: September 4, 2026
**Project**: iScan - Exam Scanner App
**Team**: Kiro + Developer
