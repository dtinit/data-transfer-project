import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-simplelogin',
  templateUrl: './simplelogin.component.html',
  styleUrls: ['./simplelogin.component.css']
})
export class SimpleLoginComponent implements OnInit {
  username: string = "";
  password: string = "";
  enableSubmit: boolean = false;
  error_text: string = "";
  constructor() { }

  ngOnInit() {
    this.toggleSubmit(false);
  }

  // Handles change in text input value
  onInputChange() {
    console.log('username: ' + this.username);
    console.log('password: ' + this.password);
    if(this.password.length > 2 && this.username.length > 2) {
      this.toggleSubmit(true);
    }
  }

  // Toggle showing submit
  private toggleSubmit(show: boolean) {
    if(this.enableSubmit != show) {
      this.enableSubmit = show;
    }
  }
}
