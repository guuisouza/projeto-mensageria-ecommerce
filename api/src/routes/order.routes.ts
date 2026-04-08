import { Router } from 'express';
import { getOrdersHandler } from '../controllers/order.controller.js';

const router = Router();

// GET /orders com paginação e ordenação
router.get('/orders', getOrdersHandler);

export default router;
