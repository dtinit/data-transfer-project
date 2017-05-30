import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ExportConfigurationComponent } from './export-configuration.component';

describe('ExportConfigurationComponent', () => {
  let component: ExportConfigurationComponent;
  let fixture: ComponentFixture<ExportConfigurationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ExportConfigurationComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExportConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
