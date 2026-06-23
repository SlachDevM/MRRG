/**
 * Sort dashboard pool jobs (pending / to-be-fixed) for display.
 * Higher priorityLevel first (4 = urgent), then jobDate, then id.
 */
export function sortPoolJobs(jobs) {
  return [...jobs].sort((a, b) => {
    const priorityDiff = (b.priorityLevel ?? 0) - (a.priorityLevel ?? 0);
    if (priorityDiff !== 0) {
      return priorityDiff;
    }

    const dateA = a.jobDate || '';
    const dateB = b.jobDate || '';
    if (dateA !== dateB) {
      return dateA.localeCompare(dateB);
    }

    return (a.id ?? 0) - (b.id ?? 0);
  });
}
