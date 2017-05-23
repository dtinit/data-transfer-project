import { WtDemoPage } from './app.po';

describe('wt-demo App', () => {
  let page: WtDemoPage;

  beforeEach(() => {
    page = new WtDemoPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
