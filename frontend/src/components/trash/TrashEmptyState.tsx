import { Link } from "react-router-dom";

export function TrashEmptyState() {
  return (
    <div className="trash-empty-state">
      <h2>Sua lixeira está vazia</h2>
      <p>As histórias movidas para a lixeira aparecerão aqui durante 7 dias.</p>
      <Link to="/" className="btn-primary">
        Voltar para a biblioteca
      </Link>
    </div>
  );
}
