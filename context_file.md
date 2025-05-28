# 📱 FitGymTrack - Progetto Porting iOS

## 🎯 OBIETTIVO
Portare app Android **FitGymTrack** (fitness tracking) su iOS usando **Compose Multiplatform**

## 📊 SITUAZIONE ATTUALE
- **App:** Kotlin + Jetpack Compose (moderna architettura)
- **Utenti:** Paio di utenti attivi, non ancora su Play Store
- **Strategia:** Repository separato (zero rischi per Android)
- **Problema:** Sviluppatore senza Mac (useremo GitHub Actions/cloud)

## 🏗️ STACK TECNICO ANDROID
```kotlin
// Dipendenze principali identificate:
- Jetpack Compose + Material3
- Retrofit + OkHttp (networking)
- Navigation Compose
- DataStore (persistenza)
- Coil (immagini)
- Coroutines
- PayPal (activity dedicata)
- Camera + Media permissions
```

## 📁 STRUTTURA ANALISI
### 🟢 VERDE (Portabile 100%)
*Files da aggiornare man mano*

### 🟡 GIALLO (Portabile con adattamenti)
*Files da aggiornare man mano*

### 🔴 ROSSO (Platform-specific)
- PayPal integration (diverso SDK iOS)
- Camera/Media permissions (diverse API iOS)
- Deep links (sintassi diversa)

---

## 📋 STATO AVANZAMENTO

### ✅ COMPLETATO
- [x] Analisi build.gradle e architettura
- [x] Identificazione stack tecnico
- [x] Strategia repository separato definita

### 🔄 IN CORSO
- [ ] Analisi files models/
- [ ] Analisi files utils/
- [ ] Analisi UI components

### ⏭️ PROSSIMI STEP
1. Analizzare cartella models/
2. Analizzare cartella utils/
3. Setup repository multiplatform
4. Migrazione graduale

---

## 💡 NOTE IMPORTANTI
- **Mai toccare main branch Android**
- **Repository completamente separato**
- **Analisi graduale cartella per cartella**
- **Focus su riutilizzo massimo codice**

---

*Ultima modifica: Prima analisi - Configurazione progetto*