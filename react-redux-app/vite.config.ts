import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server runs on :5173, which the Spring Boot CorsConfig already allows.
// The app calls the API cross-origin on purpose, to make the React -> micro-service
// boundary explicit (that boundary is what the Meteor migration later replaces).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
})
