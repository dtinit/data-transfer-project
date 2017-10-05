import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SimpleLoginComponent } from './simplelogin.component';

describe('SimpleLoginComponent', () => {
  let component: SimpleLoginComponent;
  let fixture: ComponentFixture<SimpleloginComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SimpleLoginComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SimpleLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
