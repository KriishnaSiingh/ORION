# Orion Insight Hub

Build a complete, production-quality frontend for an enterprise AI platform called **Orion Intelligence** (also known as Cognara). This is a Palantir-style Knowledge Graph + Decision Intelligence platform.

### Tech Stack

- React + TypeScript

- Tailwind CSS

- shadcn/ui components

- Lucide icons

- React Router

- TanStack Query (React Query)

- Zustand (state management)

- Recharts (for charts)

- React Flow (for graph visualizations and ontology builder)

- Dark mode + Light mode support

- Fully responsive (desktop-first, but usable on tablet)

### Design Style

- Extremely clean, modern, professional enterprise UI

- Inspired by Palantir, Linear, Notion, and Vercel

- Prefer dark mode as default

- Subtle borders, excellent spacing, high-quality typography

- Smooth animations and micro-interactions

- Use a sophisticated color palette (deep slate/zinc backgrounds, electric blue/indigo accents, subtle emerald/amber for success/warning)

### Main Application Structure

Create a full application with the following layout:

**1. Authentication Pages**

- Login page

- Register page

- Clean, centered, minimal design

**2. Main App Shell**

- Left sidebar (collapsible)

- Top navbar with search, notifications, user profile, theme toggle

- Main content area

**Sidebar Navigation items:**

- Dashboard

- Knowledge Explorer

- Investigation Boards

- Ontology Manager

- Data Ingestion

- Applications

- AI Assistant

- Settings

- Admin (only for admins)

### Core Pages & Features to Build

**A. Dashboard**

- High-level stats (Total Objects, Links, Pipelines, Active Investigations)

- Recent activity feed

- Quick actions

- Charts (objects created over time, search volume, etc.)

**B. Knowledge Explorer**

- Global search bar (hybrid search)

- Object list/table view with filters

- Object Detail page:

  - Properties

  - Linked objects

  - Interactive graph visualization (using React Flow)

  - Lineage tab

  - Activity / History

- Ability to create new objects manually

**C. Investigation Boards**

- List of boards

- Board canvas (like a real-time whiteboard)

- Ability to pin objects, add notes, draw connections

- Comments and collaboration features

- Clean, spacious UI

**D. Ontology Manager**

- List of Object Types

- Create / Edit Object Type

- Manage Properties (add, edit, reorder)

- Manage Link Types

- Visual ontology graph view (React Flow)

- Version history

**E. Data Ingestion**

- Pipeline list

- Create new pipeline

- Source configuration

- Mapping interface (map source fields → Ontology properties)

- Pipeline run history and logs

- Status indicators

**F. Applications (App Builder)**

- List of applications

- Application Builder interface (drag-and-drop style)

- Widget library (Table, Detail View, Graph, Form, KPI, Timeline, etc.)

- Preview mode

- Publish / Draft status

**G. AI Assistant**

- Chat interface (like ChatGPT but enterprise-grade)

- Show sources and evidence for every answer

- Ability to click on referenced objects

- Conversation history sidebar

- “Investigate” button that can create a board from findings

**H. Settings**

- Profile settings

- Tenant settings

- API keys

- Team members & roles

- Appearance (theme)

### Important UI Components to Include

- Advanced data tables (sorting, filtering, pagination)

- Command palette (Cmd+K)

- Beautiful empty states

- Loading skeletons

- Toast notifications

- Modal dialogs

- Slide-over panels

- Graph visualizations

- Status badges

- Permission-aware UI (hide things based on role)

### Quality Requirements

- Pixel-perfect spacing and alignment

- Excellent dark mode

- Consistent design system

- Smooth transitions

- Professional enterprise feel (not playful or childish)

- Make it look like a real $100M+ product

Please generate the full frontend with proper folder structure, reusable components, and clean code. Start with the app shell + navigation + dashboard, then build out all the major pages.generate the initial version.

Then you can refine page-by-page build it in free limit at once

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/2b79b4ae-21b9-4773-b289-ebc28b4b24a9).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
# ORION---SD-AJP-SaaS
