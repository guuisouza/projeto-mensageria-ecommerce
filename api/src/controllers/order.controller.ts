import type { Request, Response } from 'express';
import { getOrders } from '../services/order.service.js';

export async function getOrdersHandler(req: Request, res: Response) {
	try {
		const page = parseInt(req.query.page as string) || 1;
		const limit = parseInt(req.query.limit as string) || 10;
		const order = (req.query.order as string) === 'asc' ? 'asc' : 'desc';

		const result = await getOrders({ page, limit, order });
		return res.status(200).json(result);
	} catch (error) {
		return res.status(500).json({ error: 'Erro ao buscar pedidos', details: error });
	}
}
