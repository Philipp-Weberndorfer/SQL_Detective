import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, api, type CaseDetail } from '../api';
import { useAuth } from '../auth';
import BriefingView from '../components/case/BriefingView';
import CaseWorkspace from '../components/case/CaseWorkspace';

/**
 * Ein geöffneter Fall. Beim ersten Öffnen steht das Briefing vor der
 * Ermittlung (F12), danach geht es direkt zum Editor.
 */
export default function CasePage() {
  const { caseId } = useParams<{ caseId: string }>();
  const { sessionExpired } = useAuth();
  const [detail, setDetail] = useState<CaseDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showBriefing, setShowBriefing] = useState(false);
  const [starting, setStarting] = useState(false);

  const fail = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) {
        sessionExpired();
      } else {
        setError(err instanceof ApiError ? err.message : 'Der Fall konnte nicht geladen werden.');
      }
    },
    [sessionExpired],
  );

  useEffect(() => {
    setDetail(null);
    setError(null);
    api
      .caseDetail(caseId!)
      .then((d) => {
        setDetail(d);
        setShowBriefing(d.status === 'AVAILABLE');
      })
      .catch(fail);
  }, [caseId, fail]);

  async function continueInvestigation() {
    if (detail?.status === 'AVAILABLE') {
      setStarting(true);
      try {
        setDetail(await api.startCase(caseId!));
      } catch (err) {
        fail(err);
        return;
      } finally {
        setStarting(false);
      }
    }
    setShowBriefing(false);
  }

  if (error) {
    return (
      <div className="card">
        <p className="error-box" role="alert">
          {error}
        </p>
        <p>
          <Link to="/faelle">Zur Fallübersicht</Link>
        </p>
      </div>
    );
  }
  if (!detail) {
    return <p className="centered muted">Lade …</p>;
  }
  if (showBriefing) {
    return (
      <BriefingView
        detail={detail}
        firstTime={detail.status === 'AVAILABLE'}
        onContinue={continueInvestigation}
        busy={starting}
      />
    );
  }
  return <CaseWorkspace detail={detail} onShowBriefing={() => setShowBriefing(true)} />;
}
