import express from 'express';
import cors from 'cors';

const app = express();

app.use(cors());
app.use(express.json());

// Coloca debaixo dessa linha aqui a rotda(route) de orders quando ficar pronto'

app.get('/health', (req, res) => {
  res.status(200).json({ status: 'API online!' });
});

export default app;
