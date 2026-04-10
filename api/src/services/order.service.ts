
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
				item_pedido: {
					include: {
						produto: {
							include: {
								subcategoria: {
									include: {
										categoria: true,
									},
								},
							},
						},
					},
				},
				envio: true,
				pagamento: true,
				metadata: true,
			},
		}),
		prisma.pedido.count(),
	]);

	// Mapeia para o formato do payload
	const data = orders.map((order: any) => {
		// Calcula total de cada item
		const items = order.item_pedido.map((item: any) => {
			const unit_price = Number(item.unit_price);
			const quantity = item.quantity;
			const total = unit_price * quantity;
			return {
				id: item.id,
				product_id: item.produto_id,
				product_name: item.produto?.nome,
				unit_price,
				quantity,
				category: item.produto?.subcategoria?.categoria
					? {
							id: item.produto.subcategoria.categoria.id,
							name: item.produto.subcategoria.categoria.nome,
							sub_category: item.produto.subcategoria
								? {
										id: item.produto.subcategoria.id,
										name: item.produto.subcategoria.nome,
									}
								: null,
						}
					: null,
				total,
			};
		});
		// Calcula total do pedido
		const totalOrder = items.reduce((sum: number, item: any) => sum + item.total, 0);

		return {
			uuid: order.uuid,
			created_at: order.created_at,
			channel: order.channel,
			total: totalOrder,
			status: order.status,
			customer: {
				id: order.cliente?.id,
				name: order.cliente?.nome,
				email: order.cliente?.email,
				document: order.cliente?.documento,
			},
			seller: {
				id: order.seller?.id,
				name: order.seller?.nome,
				city: order.seller?.cidade,
				state: order.seller?.estado,
			},
			items,
			shipment: order.envio
				? {
						carrier: order.envio.carrier,
						service: order.envio.service,
						status: order.envio.status,
						tracking_code: order.envio.tracking_code,
					}
				: null,
			payment: order.pagamento
				? {
						method: order.pagamento.method,
						status: order.pagamento.status,
						transaction_id: order.pagamento.transaction_id,
					}
				: null,
			metadata: order.metadata
				? {
						source: order.metadata.source,
						user_agent: order.metadata.user_agent,
						ip_address: order.metadata.ip_address,
					}
				: null,
		};
	});

	return {
		data,
		page,
		limit,
		total,
		totalPages: Math.ceil(total / limit),
	};
}
