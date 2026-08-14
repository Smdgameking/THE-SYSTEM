import { TargetIcon } from './layout/icons';

export default function ComingSoon({ title, description }) {
  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">{title}</h1>
          <p className="page-subtitle">{description}</p>
        </div>
      </header>

      <div className="coming-soon">
        <div className="coming-soon-icon">
          <TargetIcon width={26} height={26} />
        </div>
        <h2 className="coming-soon-title">Module under construction</h2>
        <p className="coming-soon-text">
          This section is scheduled for a future phase of THE SYSTEM.
        </p>
      </div>
    </div>
  );
}
