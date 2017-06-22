import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { StartCopyComponent } from './start-copy.component';

describe('StartCopyComponent', () => {
  let component: StartCopyComponent;
  let fixture: ComponentFixture<StartCopyComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ StartCopyComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StartCopyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
