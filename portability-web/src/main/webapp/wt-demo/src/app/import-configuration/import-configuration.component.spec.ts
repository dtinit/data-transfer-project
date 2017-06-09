import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportConfigurationComponent } from './import-configuration.component';

describe('ImportConfigurationComponent', () => {
  let component: ImportConfigurationComponent;
  let fixture: ComponentFixture<ImportConfigurationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ImportConfigurationComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
