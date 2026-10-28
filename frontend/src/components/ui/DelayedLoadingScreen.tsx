import { useLoadingPresentation } from "../../hooks/useLoadingPresentation";
import { LoadingScreen } from "./LoadingScreen";

type DelayedLoadingScreenProps = {
  title?: string;
  description?: string;
};

export function DelayedLoadingScreen(props: DelayedLoadingScreenProps) {
  const { showLoading } = useLoadingPresentation(true);
  return showLoading ? <LoadingScreen {...props} /> : null;
}
