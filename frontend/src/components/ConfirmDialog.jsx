export default function ConfirmDialog({
  title,
  message,
  confirmLabel,
  onConfirm,
  onCancel,
  isLoading,
  isDangerous,
}) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div
        className={`confirm-dialog ${isDangerous ? 'dangerous' : ''}`}
        onClick={(e) => e.stopPropagation()}
      >
        <h2>{title}</h2>
        <p>{message}</p>
        <div className="confirm-actions">
          <button
            type="button"
            className="btn-cancel"
            onClick={onCancel}
            disabled={isLoading}
          >
            Cancel
          </button>
          <button
            type="button"
            className={`btn-confirm ${isDangerous ? 'dangerous' : ''}`}
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading ? 'Processing...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
