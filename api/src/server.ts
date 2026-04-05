import app from './app.js';
import * as dotenv from 'dotenv';

dotenv.config();

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`🚀 API rodando em: http://localhost:${PORT}`);
  console.log(`Verifique a rota: http://localhost:${PORT}/orders`);
});
