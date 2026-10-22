import { PostgreSQL, sql } from '@codemirror/lang-sql';
import { Prec } from '@codemirror/state';
import { keymap } from '@codemirror/view';
import CodeMirror from '@uiw/react-codemirror';
import { useMemo, useRef } from 'react';

interface Props {
  value: string;
  onChange: (value: string) => void;
  /** Strg+Enter bzw. Cmd+Enter */
  onRun: () => void;
}

/** SQL-Eingabe mit Syntax-Highlighting (CodeMirror, PostgreSQL-Dialekt). */
export default function SqlEditor({ value, onChange, onRun }: Props) {
  // Die Tastenkombination wird nur einmal registriert, ruft aber immer die
  // aktuelle onRun-Funktion auf.
  const onRunRef = useRef(onRun);
  onRunRef.current = onRun;

  const extensions = useMemo(
    () => [
      sql({ dialect: PostgreSQL, upperCaseKeywords: true }),
      Prec.highest(
        keymap.of([
          {
            key: 'Mod-Enter',
            run: () => {
              onRunRef.current();
              return true;
            },
          },
        ]),
      ),
    ],
    [],
  );

  return (
    <div className="sql-editor" aria-label="SQL-Editor">
      <CodeMirror
        value={value}
        onChange={onChange}
        extensions={extensions}
        minHeight="140px"
        maxHeight="320px"
        basicSetup={{ lineNumbers: true, foldGutter: false, highlightActiveLine: true }}
      />
    </div>
  );
}
