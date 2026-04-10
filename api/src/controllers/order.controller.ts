import type { Request, Response } from 'express';
// Importamos também a tipagem OrderQueryParams do service
import { getOrders, getOrderByUuid, type OrderQueryParams } from '../services/order.service.js';

export async function getOrdersHandler(req: Request, res: Response) {
	try {
		const page = parseInt(req.query.page as string) || 1;
		const limit = parseInt(req.query.limit as string) || 10;
		const order = (req.query.order as string) === 'asc' ? 'asc' : 'desc';
		
		const params: OrderQueryParams = { page, limit, order };

		if (req.query.codigoCliente) {
			params.codigoCliente = parseInt(req.query.codigoCliente as string);
		}
		
		if (req.query.codigoProduto) {
			params.codigoProduto = parseInt(req.query.codigoProduto as string);
		}
		
		if (req.query.status) {
			params.status = req.query.status as string;
		}

		// Passamos o objeto params já formatado e sem valores "undefined"
		const result = await getOrders(params);
		
		return res.status(200).json(result);
	} catch (error) {
		return res.status(500).json({ error: 'Erro ao buscar pedidos', details: error });
	}
}

// Rota para buscar apenas 1 pedido por UUID
export async function getOrderByUuidHandler(req: Request, res: Response) {
	try {
		const uuid = req.params.uuid as string;
		
		if (!uuid) {
			return res.status(400).json({ error: 'O UUID do pedido é obrigatório' });
		}
		
		const result = await getOrderByUuid(uuid);
		
		if (!result) {
			return res.status(404).json({ error: 'Pedido não encontrado' });
		}
		
		return res.status(200).json(result);
	} catch (error) {
		return res.status(500).json({ error: 'Erro ao buscar o pedido', details: error });
	}
}