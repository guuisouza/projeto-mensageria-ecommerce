import { prisma } from '../lib/prisma.js';

export interface OrderQueryParams {
	page?: number;
	limit?: number;
	order?: 'asc' | 'desc';
}

export async function getOrders({ page = 1, limit = 10, order = 'desc' }: OrderQueryParams) {
	const skip = (page - 1) * limit;
	const [orders, total] = await Promise.all([
		prisma.pedido.findMany({
			skip,
			take: limit,
			orderBy: { created_at: order },
			include: {
				cliente: true,
				seller: true,
				item_pedido: true,
				envio: true,
				pagamento: true,
				metadata: true,
			},
		}),
		prisma.pedido.count(),
	]);

	// Mapeia para o formato do payload
	const data = orders.map((order: any) => ({
		uuid: order.uuid,
		createdAt: order.created_at,
		channel: order.channel,
		status: order.status,
		customer: order.cliente,
		seller: order.seller,
		items: order.item_pedido,
		shipment: order.envio,
		payment: order.pagamento,
		metadata: order.metadata,
	}));

	return {
		data,
		page,
		limit,
		total,
		totalPages: Math.ceil(total / limit),
	};
}
