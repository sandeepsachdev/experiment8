# Tuneful — Music Discovery

A Spring Boot web app for searching music by partial artist or song name, getting
recommendations based on what you searched, and browsing charts across countries
and genres.

## Features

- **Search** by partial artist name or partial song title (Deezer search API).
- **Discovery** — click any artist or track to see similar artists and a curated
  list of tracks you may like, drawn from related artists' top tracks.
- **Charts page** showing:
  - **Apple Music** top songs across 17 countries.
  - **Deezer** top tracks filtered by 18 genres.
- **30-second previews** for most tracks, played inline.
- **REST API** under `/api/...` for the same data in JSON.

## APIs used

- **Deezer Public API** (`https://api.deezer.com`) — search, artist/track lookup,
  related artists, top tracks, genre charts. No API key required.
- **Apple Music / iTunes RSS** (`https://rss.applemarketingtools.com`) — per-country
  most-played songs feed. No API key required.

## Running locally

Requires Java 17+ and Maven (or the Maven wrapper).

```bash
mvn spring-boot:run
# or:
mvn -DskipTests package && java -jar target/musicapp-0.0.1-SNAPSHOT.jar
```

Then open <http://localhost:8080>.

## Running with Docker

```bash
docker build -t tuneful .
docker run --rm -p 8080:8080 tuneful
```

## Deploying

### Render

The repo ships with `render.yaml` — create a new web service from this repo on
[Render](https://render.com) and it will pick up the Docker config automatically.

### Railway

The repo ships with `railway.json`. Create a new project from this repo on
[Railway](https://railway.app); it will build the Dockerfile and expose
`$PORT` automatically (the app reads `PORT` and falls back to 8080 locally).

## Endpoints

| Path | Description |
| --- | --- |
| `GET /` | Home / landing page |
| `GET /search?q=...` | Web search page |
| `GET /artist/{id}` | Artist page with recommendations |
| `GET /track/{id}` | Track page with recommendations |
| `GET /charts?country=us&genre=132` | Charts page |
| `GET /api/search?q=...` | JSON search |
| `GET /api/recommendations/artist/{id}` | JSON recommendations for an artist |
| `GET /api/recommendations/track/{id}` | JSON recommendations for a track |
| `GET /api/charts/apple?country=us` | JSON Apple Music chart |
| `GET /api/charts/deezer?genre=132` | JSON Deezer chart |

## Caching

Results from external APIs are cached in-process with Caffeine for 10 minutes
(see `application.properties`) to be polite to the upstream APIs.
