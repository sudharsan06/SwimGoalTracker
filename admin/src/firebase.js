import { initializeApp } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
} from 'firebase/auth';
import {
  getDatabase,
  ref,
  get,
  set,
  update,
  remove,
  query,
  orderByKey,
  limitToFirst,
  limitToLast,
  startAfter,
  endAt,
  orderByChild,
  equalTo,
} from 'firebase/database';
import {
  getStorage,
} from 'firebase/storage';

const firebaseConfig = {
  apiKey: 'AIzaSyA4d_EXbjud3KLFZ5n0rxfV6qHvfHr7tLo',
  authDomain: 'swimmingo.firebaseapp.com',
  databaseURL: 'https://swimmingo-default-rtdb.firebaseio.com',
  projectId: 'swimmingo',
  storageBucket: 'swimmingo.firebasestorage.app',
  messagingSenderId: '280989570232',
  appId: '1:280989570232:android:ff4af04d72de21be6f104d',
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const database = getDatabase(app);
const storage = getStorage(app);

const db = database;

export {
  auth,
  db,
  database,
  storage,
  ref,
  get,
  set,
  update,
  remove,
  query,
  orderByKey,
  limitToFirst,
  limitToLast,
  startAfter,
  endAt,
  orderByChild,
  equalTo,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
};
