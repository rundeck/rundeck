type ModalLinks = {
  id?: string;
  href: string;
  css?: string;
  message?: string;
  messageCode?: string;
  js?: any;
};

type ModalButtons = {
  id?: string;
  css?: string;
  js?: any;
  message?: string;
  messageCode?: string;
};

interface JobDetailsData {
  jobName: string;
  description?: string;
  groupPath?: string;
}

export { ModalLinks, ModalButtons, JobDetailsData };
