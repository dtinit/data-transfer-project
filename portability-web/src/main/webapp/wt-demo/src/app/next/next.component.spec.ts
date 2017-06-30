import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NextComponent } from './next.component';

describe('NextComponent', () => {
  let component: NextComponent;
  let fixture: ComponentFixture<NextComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NextComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NextComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
