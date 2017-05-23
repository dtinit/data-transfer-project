import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ListServicesComponent } from './list-services.component';

describe('ListServicesComponent', () => {
  let component: ListServicesComponent;
  let fixture: ComponentFixture<ListServicesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ListServicesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListServicesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
