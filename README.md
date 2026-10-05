<p align="center">
  <img src="https://img.shields.io/badge/Security-Local%20First-success?style=for-the-badge&logo=shield&logoColor=white" alt="Local Security" />
  <img src="https://img.shields.io/badge/Built%20With-Spring%20Boot%204.1-brightgreen?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 4.1" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL%20%2F%20pgvector-blue?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Powered%20By-Ollama%20%2F%20Spring%20AI-orange?style=for-the-badge&logo=ollama&logoColor=white" alt="Ollama" />
</p>

<h1 align="center">📄 AI Resume Analyzer Backend</h1>

<p align="center">
  <strong>A Spring Boot backend for analyzing resumes using local LLMs (Ollama), PDF text extraction, and vector similarity search (pgvector).</strong>
</p>

<p align="center">
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-17-orange.svg?style=flat-square" alt="Java Version" /></a>
  <a href="https://spring.io/projects/spring-ai"><img src="https://img.shields.io/badge/Spring%20AI-1.0.0--M1-blue.svg?style=flat-square" alt="Spring AI" /></a>
  <a href="https://pdfbox.apache.org/"><img src="https://img.shields.io/badge/PDF%20Extraction-Apache%20PDFBox-red.svg?style=flat-square" alt="PDFBox" /></a>
  <a href="https://swagger.io/"><img src="https://img.shields.io/badge/API%20Docs-OpenAPI%20%2F%20Swagger-brightgreen.svg?style=flat-square" alt="OpenAPI" /></a>
</p>

---

> [!IMPORTANT]  
> **Secure-By-Design Local Inference**  
> AI Resume Analyzer uses Ollama for local embeddings (`nomic-embed-text`) and chat generation (`llama3.2:1b`). Your uploaded documents, parsed texts, and query prompts remain completely on your machine.

---

## ✨ Core Pillars & Features

*   📂 **PDF Text Extraction** — Uploads PDF resumes and extracts raw text using **Apache PDFBox**.
*   ✂️ **Text Chunking** — Splits extracted text into overlapping word-boundary windows for embedding. Overlap preserves cross-chunk context.
*   🧠 **Vector Embeddings** — Generates embeddings via Ollama's `nomic-embed-text` model and stores them in **PostgreSQL + pgvector** with an HNSW index.
*   💬 **RAG Q&A Pipeline** — Answer natural language questions about resumes using Retrieval-Augmented Generation with grounded context.
*   🎯 **Job Description Matching** — Scores candidate fit using a hybrid approach: deterministic keyword overlap + semantic vector similarity + LLM qualitative analysis.
*   💡 **Categorized Suggestions** — Generates actionable, categorized improvement recommendations (ATS formatting, skills, projects, experience).
*   🔒 **Duplicate Prevention** — Uses SHA-256 file hashing to detect and reject identical uploads.

---

## 🏗️ Architecture & Pipeline Flow

The backend orchestrates resume processing through these layers:

```text
HTTP Request
     │
     ▼
Controller Layer (/api/v1/resumes/*)
     │  (delegates, no business logic)
     ▼
Service Layer
  ├── ResumeService           (orchestrates upload pipeline)
  ├── PdfTextExtractorService (PDFBox extraction)
  ├── TextNormalizationService(clean raw PDF text)
  ├── TextChunkingService     (overlapping word-boundary chunks)
  ├── EmbeddingService        (pgvector add/search/delete)
  ├── LlmClientService        (Spring AI ChatModel wrapper)
  ├── ResumeQuestionService   (RAG Q&A pipeline)
  ├── ResumeMatchService      (hybrid JD matching)
  └── ResumeSuggestionService (categorized suggestions)
     │
     ▼
Repository Layer (Spring Data JPA)
     │
     ▼
PostgreSQL + pgvector
```

---

## ⚡ Quick Start

Follow these steps to set up and run the service locally:

### 1. Configure Environment Variables
Copy the template configuration file:
```bash
cp .env.example .env
# Edit .env to adjust connection strings or model defaults if necessary
```

### 2. Pull Ollama Local Models
Ensure Ollama is running, then download the chat model and embedding models:
```bash
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

### 3. Start PostgreSQL + pgvector + Redis
Start infrastructure services using Docker Compose:
```bash
docker-compose up postgres redis -d
```

### 4. Run the Backend Service
Execute the Spring Boot application:
```bash
mvn spring-boot:run
```
### 5. Access Observability & Documentation Endpoints
Once the application is running on port 8080, you can access the following auto-generated developer portals:

*   **Swagger UI (Interactive API Docs):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
    *   *Provides a complete OpenAPI 3 specification and a web-based UI to test all endpoints.*
*   **Actuator Health Check:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
    *   *Returns the liveness state of the application and its connected database (useful for CI/CD readiness probes).*
*   **Actuator Application Info:** [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)
    *   *Returns metadata about the deployed build.*
---

## 💻 API Endpoints & Cheat Sheet

| Method | Path | Description |
| :--- | :--- | :--- |
| **`POST`** | `/api/v1/auth/signup` | Register a new user |
| **`POST`** | `/api/v1/auth/login` | Login (returns JWT) |
| **`POST`** | `/api/v1/resumes/upload` | Upload resume PDF |
| **`GET`** | `/api/v1/resumes` | List resumes (paginated) |
| **`GET`** | `/api/v1/resumes/{id}` | Get metadata |
| **`GET`** | `/api/v1/resumes/{id}/text` | Get raw parsed text |
| **`DELETE`** | `/api/v1/resumes/{id}` | Delete resume & embeddings |
| **`POST`** | `/api/v1/resumes/{id}/ask` | Ask questions (RAG) |
| **`POST`** | `/api/v1/resumes/{id}/match` | Score against Job Description |
| **`POST`** | `/api/v1/resumes/{id}/suggestions` | Get ATS recommendations |

---

## 📖 Sample Request Workflows

<details>
<summary>📺 Click to expand: Resume Upload Example</summary>

```bash
curl -X POST http://localhost:8080/api/v1/resumes/upload \
  -F "file=@/path/to/resume.pdf"
```
</details>

<details>
<summary>📺 Click to expand: Ask Questions (RAG QA)</summary>

```bash
curl -X POST http://localhost:8080/api/v1/resumes/1/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What programming languages does the candidate know?"}'
```
</details>

<details>
<summary>📺 Click to expand: Match Job Description</summary>

```bash
curl -X POST http://localhost:8080/api/v1/resumes/1/match \
  -H "Content-Type: application/json" \
  -d '{"jobDescription": "We are looking for a Java backend developer with Spring Boot and PostgreSQL experience."}'
```
</details>

<details>
<summary>📺 Click to expand: Get Improvement Suggestions</summary>

```bash
curl -X POST "http://localhost:8080/api/v1/resumes/1/suggestions?jobDescription=Senior+Java+Engineer" 
```
</details>

---

## ⚙️ Advanced Configurations & Exclusions

Active environment values can be set via `.env` or direct shell profiles:

<details>
<summary>📋 Click to view: System Environment Properties</summary>

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:postgresql://localhost:5440/resume_analyzer` | Connection url for PostgreSQL |
| `DB_USERNAME` | `postgres` | Postgres user |
| `DB_PASSWORD` | `changeme` | Postgres database secret |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Target Ollama Server instance |
| `OLLAMA_CHAT_MODEL` | `llama3.2:1b` | AI Chat LLM name |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | AI Embeddings Vectorizer LLM name |
| `SERVER_PORT` | `8080` | Port allocation |

</details>

---

## 🔒 Limitations & Roadmap

<details>
<summary>⚠️ Click to view: Known Limitations</summary>

-   **Scanned PDFs** — OCR is not yet implemented. Image-only PDFs will throw parsing exceptions (Tesseract4J integration planned).
-   **Security Credentials** — No authentication layers (JWT, OAuth) are set up. Use strictly inside local environments.
-   **LLM Consistency** — Formatting inconsistencies can happen with small local parameters (llama3.2:1b). A robust regex-based keyword engine acts as a fallback for scoring endpoints.
-   **No Rate Limits** — Recommended to stack behind Nginx or Spring Cloud Gateway when scaling.

</details>

<details>
<summary>🚀 Click to view: Future Roadmap Improvements</summary>

- [ ] Add OCR capabilities using Tesseract4J.
- [ ] Incorporate JWT/API token authentication mechanisms via Spring Security.
- [ ] Implement asynchronous queueing for large document processing with status polling.
- [ ] Add Testcontainers testing architecture.
- [ ] Set up a Redis layer to cache frequent Q&A vector retrievals.
- [ ] Add a resume comparing and ranking endpoint.

</details>

---

## 📄 License

This project is licensed under the MIT License.
