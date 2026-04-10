import { Router } from 'express';
import { getOrdersHandler, getOrderByUuidHandler } from '../controllers/order.controller.js';

const router = Router();

// GET /orders?codigoCliente=X&codigoProduto=Y&status=Z
router.get('/orders', getOrdersHandler);

// GET /orders/{uuid}
router.get('/orders/:uuid', getOrderByUuidHandler);

export default router;