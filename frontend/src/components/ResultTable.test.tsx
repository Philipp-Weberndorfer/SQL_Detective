import { render, screen, within } from '@testing-library/react';
import ResultTable from './ResultTable';

describe('ResultTable', () => {
  it('zeigt Spaltenüberschriften und Zeilen', () => {
    render(
      <ResultTable
        result={{
          columns: ['first_name', 'last_name'],
          rows: [
            ['Max', 'Mustermann'],
            ['Anna', null],
          ],
          rowCount: 2,
          truncated: false,
          durationMs: 4,
        }}
      />,
    );

    const headers = screen.getAllByRole('columnheader').map((th) => th.textContent);
    expect(headers).toEqual(['first_name', 'last_name']);

    const rows = screen.getAllByRole('row');
    expect(within(rows[1]).getByText('Mustermann')).toBeInTheDocument();
    expect(within(rows[2]).getByText('NULL')).toBeInTheDocument();
    expect(screen.getByText(/2 Zeilen/)).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('weist auf das Zeilenlimit hin', () => {
    render(
      <ResultTable
        result={{ columns: ['g'], rows: Array.from({ length: 500 }, (_, i) => [i]), rowCount: 500, truncated: true, durationMs: 9 }}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('auf 500 Zeilen begrenzt');
  });

  it('meldet ein leeres Ergebnis ohne Fehler', () => {
    render(<ResultTable result={{ columns: ['id'], rows: [], rowCount: 0, truncated: false, durationMs: 1 }} />);
    expect(screen.getByText('Keine Zeilen gefunden.')).toBeInTheDocument();
    expect(screen.getByRole('columnheader')).toHaveTextContent('id');
  });
});
