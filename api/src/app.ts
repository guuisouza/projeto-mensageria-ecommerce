import express from 'express';
import cors from 'cors';

const app = express();

app.use(cors());
app.use(express.json());


import orderRoutes from './routes/order.routes.js';
app.use(orderRoutes);

app.get('/health', (req, res) => {
  res.status(200).json({ status: 'API online!' });
});

export default app;
