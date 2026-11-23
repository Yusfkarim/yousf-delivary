export function SkeletonCard() {
  return (
    <div className="glass space-y-3 p-3.5">
      <div className="flex items-center justify-between gap-3">
        <div className="skeleton h-4 w-32" />
        <div className="skeleton h-5 w-20 rounded-full" />
      </div>
      <div className="skeleton h-14 w-full" />
      <div className="skeleton h-10 w-full" />
      <div className="grid grid-cols-4 gap-1.5">
        <div className="skeleton h-10" />
        <div className="skeleton h-10" />
        <div className="skeleton h-10" />
        <div className="skeleton h-10" />
      </div>
    </div>
  );
}
