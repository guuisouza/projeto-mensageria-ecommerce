import { prisma } from '../lib/prisma.js';

export interface OrderQueryParams {
	page?: number;
	limit?: number;
	order?: 'asc' | 'desc';
	codigoCliente?: number;
	codigoProduto?: number;
	status?: string;
}

function formatOrder(order: any) {
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
}

// Objeto de include padrão para o Prisma
const includeRelations = {
	cliente: true,
	seller: true,
	item_pedido: {
		include: {
			produto: {
				include: { subcategoria: { include: { categoria: true } } },
			},
		},
	},
	envio: true,
	pagamento: true,
	metadata: true,
};

export async function getOrders({ page = 1, limit = 10, order = 'desc', codigoCliente, codigoProduto, status }: OrderQueryParams) {
	const skip = (page - 1) * limit;
	
	// Monta os filtros dinamicamente
	const where: any = {};
	
	if (codigoCliente) {
		where.cliente_id = codigoCliente;
	}
	
	if (status) {
		where.status = status;
	}
	
	if (codigoProduto) {
		// Busca pedidos que tenham PELO MENOS UM (some) item_pedido com o id do produto filtrado
		where.item_pedido = {
			some: {
				produto_id: codigoProduto,
			}
		};
	}

	const [orders, total] = await Promise.all([
		prisma.pedido.findMany({
			where,
			skip,
			take: limit,
			orderBy: { created_at: order },
			include: includeRelations,
		}),
		prisma.pedido.count({ where }),
	]);

	const data = orders.map(formatOrder);

	return {
		data,
		page,
		limit,
		total,
		totalPages: Math.ceil(total / limit),
	};
}

export async function getOrderByUuid(uuid: string) {
	const order = await prisma.pedido.findUnique({
		where: { uuid },
		include: includeRelations,
	});

	if (!order) return null;

	return formatOrder(order);
}