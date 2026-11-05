import { useParams } from 'react-router-dom';
import EditorPage from './EditorPage';

/** Ein geöffneter Fall. */
export default function CasePage() {
  const { caseId } = useParams<{ caseId: string }>();
  return <EditorPage caseId={caseId!} />;
}
